package com.loopify.mainservice.service.notification.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopify.mainservice.dto.NotificationDTO;
import com.loopify.mainservice.enums.NotificationType;
import com.loopify.mainservice.exception.AppException;
import com.loopify.mainservice.model.Notification;
import com.loopify.mainservice.notification.BaseNotification;
import com.loopify.mainservice.notification.CommentNotification;
import com.loopify.mainservice.notification.FollowNotification;
import com.loopify.mainservice.repository.notification.NotificationRepository;
import com.loopify.mainservice.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final RabbitTemplate rabbitTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${rabbitmq.exchange.notifications}")
    private String notificationExchange;

    @Value("${rabbitmq.routing-key.follow}")
    private String followRoutingKey;

    @Value("${rabbitmq.routing-key.comment}")
    private String commentRoutingKey;

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000))
    @Transactional
    public void sendFollowNotification(FollowNotification followNotification) {
        String redisKey = "notification:user:" + followNotification.getTargetUserId();
        try {
            // 1. Persist to MySQL
            Notification notification = Notification.builder()
                    .actionUserId(followNotification.getActionUserId())
                    .targetUserId(followNotification.getTargetUserId())
                    .type(NotificationType.FOLLOW)
                    .content("User " + followNotification.getActionUserId() + " followed you")
                    .build();

            saveToDb(notification, followNotification, redisKey);

            // 4. Send to RabbitMQ
            rabbitTemplate.convertAndSend(notificationExchange, followRoutingKey,
                    objectMapper.writeValueAsString(followNotification));
            log.info("Follow notification sent: User {} followed User {}",
                    followNotification.getActionUserId(), followNotification.getTargetUserId());
        } catch (Exception e) {
            // 如果 RabbitMQ 发送失败，回滚 Redis 操作
            redisTemplate.opsForList().remove(redisKey, 1, followNotification);
            log.error("Failed to send follow notification, rolled back Redis operation", e);
            throw new AppException(e.getMessage());
        }
    }

    @Override
    @Transactional
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void sendCommentNotification(CommentNotification commentNotification) {
        String redisKey = "notification:user:" + commentNotification.getTargetUserId();
        try {
            // 1. Persist to MySQL
            Notification notification = Notification.builder()
                    .actionUserId(commentNotification.getActionUserId())
                    .targetUserId(commentNotification.getTargetUserId())
                    .type(NotificationType.COMMENT)
                    .content("User " + commentNotification.getActionUserId() +
                            " commented on your post: " + commentNotification.getPreviewText())
                    .build();

            saveToDb(notification, commentNotification, redisKey);

            // 4. Send to RabbitMQ
            rabbitTemplate.convertAndSend(notificationExchange, commentRoutingKey,
                    objectMapper.writeValueAsString(commentNotification));

            log.info("Comment notification sent: User {} commented on post {}",
                    commentNotification.getActionUserId(), commentNotification.getPostId());

        } catch (Exception e) {
            redisTemplate.opsForList().remove(redisKey, 1, commentNotification);
            log.error("Failed to send comment notification", e);
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    @Transactional
    public void markNotificationAsRead(Long notificationId, Long userId) {
        // 1. Update in MySQL
        int updated = notificationRepository.markAsRead(notificationId, userId);

        if (updated > 0) {
            // 2. Update in Redis
            String readKey = "notification:read:" + userId;
            redisTemplate.opsForSet().add(readKey, notificationId);

            // 3. Decrement unread count
            decrementUnreadCount(userId);

            log.info("Notification {} marked as read for user {}", notificationId, userId);
        } else {
            log.warn("Failed to mark notification {} as read for user {}", notificationId, userId);
        }
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        // 1. Get all unread notifications
        List<Notification> unreadNotifications = notificationRepository.findByTargetUserIdAndReadIsFalseOrderByCreatedAtDesc(userId);

        // 2. Mark all as read in MySQL
        int updated = notificationRepository.markAllAsRead(userId);

        if (updated > 0) {
            // 3. Update Redis read set
            String readKey = "notification:read:" + userId;
            for (Notification notification : unreadNotifications) {
                redisTemplate.opsForSet().add(readKey, notification.getId());
            }

            // 4. Reset unread count
            String unreadCountKey = "notification:unread:count:" + userId;
            redisTemplate.delete(unreadCountKey);

            log.info("{} notifications marked as read for user {}", updated, userId);
        }
    }

    @Override
    public long getUnreadCount(Long userId) {
        String unreadCountKey = "notification:unread:count:" + userId;

        // Try to get from Redis first
        Object count = redisTemplate.opsForValue().get(unreadCountKey);
        if (count != null) {
            return Long.parseLong(count.toString());
        }

        // If not in Redis, get from database and update Redis
        long dbCount = notificationRepository.countByTargetUserIdAndReadIsFalse(userId);
        redisTemplate.opsForValue().set(unreadCountKey, dbCount);

        return dbCount;
    }

    @Override
    public Page<NotificationDTO> getNotifications(Long userId, int page, int size) {
        List<NotificationDTO> result = new ArrayList<>();
        String redisKey = "notification:user:" + userId;

        long startIndex = (long) page * size;
        long endIndex = startIndex + size - 1;

        List<Object> redisNotifications = redisTemplate.opsForList().range(redisKey, startIndex, endIndex);
        boolean needSupplementFromDb = false;
        if (redisNotifications != null && !redisNotifications.isEmpty()) {
            // Redis中有数据，转换为DTO
            for (Object obj : redisNotifications) {
                if (obj instanceof BaseNotification notification) {
                    NotificationDTO dto = convertToDTO(notification);

                    // 检查是否已读
                    String readKey = "notification:read:" + userId;
                    boolean isRead = Boolean.TRUE.equals(
                            redisTemplate.opsForSet().isMember(readKey, notification.getNotificationId()));
                    dto.setRead(isRead);

                    result.add(dto);
                }
            }

            // 如果Redis中获取的数据少于请求的数量，需要从数据库补充
            if (redisNotifications.size() < size) {
                needSupplementFromDb = true;
            }
        } else {
            // Redis中没有数据，需要从数据库加载
            needSupplementFromDb = true;
        }

        // 2. 如果Redis中数据不足或没有，从MySQL补充
        if (needSupplementFromDb || page > 0) {  // 对于非首页或Redis数据不足的情况查询数据库
            Page<Notification> dbNotifications = notificationRepository.findByTargetUserIdOrderByCreatedAtDesc(
                    userId, PageRequest.of(page, size));

            // 如果是第一页且Redis数据为空，将数据缓存到Redis
            if (page == 0 && (redisNotifications == null || redisNotifications.isEmpty())) {
                List<Notification> content = dbNotifications.getContent();

                // 转换为BaseNotification对象并缓存到Redis
                for (Notification notification : content) {
                    BaseNotification baseNotification;

                    if (notification.getType() == NotificationType.FOLLOW) {
                        baseNotification = new FollowNotification(
                                notification.getActionUserId(),
                                notification.getTargetUserId());
                    } else if (notification.getType() == NotificationType.COMMENT) {
                        CommentNotification commentNotification = new CommentNotification();
                        commentNotification.setActionUserId(notification.getActionUserId());
                        commentNotification.setTargetUserId(notification.getTargetUserId());

                        String previewText = notification.getContent().substring(notification.getContent().indexOf(":") + 1).trim();
                        commentNotification.setPreviewText(previewText);
                        baseNotification = commentNotification;
                    } else {
                        continue; // 跳过未知类型
                    }

                    baseNotification.setNotificationId(notification.getId());
                    baseNotification.setTimestamp(notification.getCreatedAt());

                    // 缓存到Redis
                    redisTemplate.opsForList().rightPush(redisKey, baseNotification);
                }

                // 修剪列表并设置过期时间
                redisTemplate.opsForList().trim(redisKey, 0, 99); // 保留最近100条
                redisTemplate.expire(redisKey, 7, TimeUnit.DAYS);

                // 直接使用数据库查询结果
                return dbNotifications.map(this::convertToDTO);
            }

            // 如果Redis中有部分数据但不足，只返回Redis中的数据
            if (!result.isEmpty()) {
                return new PageImpl<>(result, PageRequest.of(page, size),
                        (long) page * size + result.size() + (dbNotifications.hasNext() ? 1 : 0));
            }

            // 其他情况返回数据库查询结果
            return dbNotifications.map(this::convertToDTO);
        }

        // 3. 返回从Redis获取的完整结果
        // 计算总条数 (这里可以优化，如缓存总数)
        long total = notificationRepository.countByTargetUserId(userId);
        return new PageImpl<>(result, PageRequest.of(page, size), total);
    }

    @Override
    public List<NotificationDTO> getUnreadNotifications(Long userId) {
        // 1. 首先查询Redis中的未读计数
        String unreadCountKey = "notification:unread:count:" + userId;
        Object unreadCountObj = redisTemplate.opsForValue().get(unreadCountKey);
        long unreadCount = unreadCountObj != null ? Long.parseLong(unreadCountObj.toString()) : 0;

        if (unreadCount == 0) {
            // 没有未读消息，直接返回空列表
            return Collections.emptyList();
        }

        // 2. 获取Redis中的通知列表
        String redisKey = "notification:user:" + userId;
        List<Object> redisNotifications = redisTemplate.opsForList().range(redisKey, 0, -1);

        // 3. 获取已读通知ID集合
        String readKey = "notification:read:" + userId;
        Set<Object> readSet = redisTemplate.opsForSet().members(readKey);
        Set<String> readIds = readSet != null ?
                readSet.stream().map(Object::toString).collect(Collectors.toSet()) :
                Collections.emptySet();

        List<NotificationDTO> unreadNotifications = new ArrayList<>();

        // 4. 过滤未读通知
        if (redisNotifications != null && !redisNotifications.isEmpty()) {
            for (Object obj : redisNotifications) {
                if (obj instanceof BaseNotification notification) {
                    if (!readIds.contains(notification.getNotificationId().toString())) {
                        unreadNotifications.add(convertToDTO(notification));

                        // 如果已经找到所有未读通知，提前结束
                        if (unreadNotifications.size() >= unreadCount) {
                            break;
                        }
                    }
                }
            }
        }

        // 5. 如果Redis中未读通知数量与计数不符，从数据库补充
        if (unreadNotifications.size() < unreadCount) {
            // 获取已找到的未读通知ID
            Set<Long> foundIds = unreadNotifications.stream()
                    .map(NotificationDTO::getId)
                    .collect(Collectors.toSet());

            // 从数据库查询剩余未读通知
            List<Notification> dbUnread = notificationRepository
                    .findByTargetUserIdAndReadIsFalseAndIdNotInOrderByCreatedAtDesc(
                            userId, foundIds, PageRequest.of(0, (int)(unreadCount - unreadNotifications.size())));

            for (Notification notification : dbUnread) {
                unreadNotifications.add(convertToDTO(notification));
            }
        }

        return unreadNotifications;
    }

    // 新增从BaseNotification转换为NotificationDTO的方法
    private NotificationDTO convertToDTO(BaseNotification notification) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(notification.getNotificationId());
        dto.setType(notification.getType());
        dto.setActionUserId(notification.getActionUserId());
        dto.setCreatedAt(notification.getTimestamp());

        // 根据类型设置内容
        if (notification instanceof FollowNotification followNotification) {
            dto.setContent("User " + followNotification.getActionUserId() + " followed you");
        } else if (notification instanceof CommentNotification commentNotification) {
            dto.setContent("User " + commentNotification.getActionUserId() +
                    " commented on your post: " + commentNotification.getPreviewText());
        }

        return dto;
    }

    // 从Notification实体转换为NotificationDTO的方法
    private NotificationDTO convertToDTO(Notification notification) {
        return NotificationDTO.builder()
                .id(notification.getId())
                .type(notification.getType().name())
                .actionUserId(notification.getActionUserId())
                .content(notification.getContent())
                .read(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    private void saveToDb(Notification notification, BaseNotification baseNotification, String redisKey) {
        notification = notificationRepository.save(notification);

        // Update the notification ID from database
        baseNotification.setNotificationId(notification.getId());

        // 2. Cache in Redis

        redisTemplate.opsForList().leftPush(redisKey, baseNotification);
        redisTemplate.opsForList().trim(redisKey, 0, 99); // Keep last 100

        // Set TTL for Redis cache
        redisTemplate.expire(redisKey, 7, TimeUnit.DAYS);

        // 3. Update unread count in Redis
        incrementUnreadCount(baseNotification.getTargetUserId());
    }

    private void incrementUnreadCount(Long userId) {
        String unreadCountKey = "notification:unread:count:" + userId;
        redisTemplate.opsForValue().increment(unreadCountKey);
    }

    private void decrementUnreadCount(Long userId) {
        String unreadCountKey = "notification:unread:count:" + userId;
        Long count = redisTemplate.opsForValue().decrement(unreadCountKey);

        // Ensure count doesn't go below 0
        if (count != null && count < 0) {
            redisTemplate.opsForValue().set(unreadCountKey, 0);
        }
    }
}
