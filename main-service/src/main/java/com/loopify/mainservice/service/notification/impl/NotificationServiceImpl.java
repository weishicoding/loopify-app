package com.loopify.mainservice.service.notification.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopify.mainservice.model.user.User;
import com.loopify.mainservice.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {
    private final RabbitTemplate rabbitTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${rabbitmq.exchange.notifications}")
    private String notificationExchange;

    @Value("${rabbitmq.routing-key.follow}")
    private String followRoutingKey;

    @Override
    public void sendFollowNotification(User follower, User following) {
        try {
            String notificationId = UUID.randomUUID().toString();

            FollowNotification notification = new FollowNotification();
            notification.setNotificationId((long) notificationId.hashCode());
            notification.setActionUserId(follower.getId());
            notification.setActionUserNickname(follower.getNickname());
            notification.setActionUserAvatar(follower.getAvatarUrl());
            notification.setTargetUserId(following.getId());
            notification.setTimestamp(LocalDateTime.now());

            sendNotification(notification, followRoutingKey);

            log.info("Follow notification sent: User {} followed User {}", follower.getId(), following.getId());
        } catch (Exception e) {
            log.error("Failed to send follow notification", e);
        }
    }

    // send others notifications same type

    private void sendNotification(BaseNotification notification, String routingKey) throws Exception {
        // Send to RabbitMQ for async processing
        rabbitTemplate.convertAndSend(notificationExchange, routingKey,
                objectMapper.writeValueAsString(notification));

        // Store in Redis for quick access
        String redisKey = "notification:user:" + notification.getTargetUserId();
        redisTemplate.opsForList().leftPush(redisKey, notification);
        redisTemplate.opsForList().trim(redisKey, 0, 99); // Keep only the 100 most recent
    }

    @Override
    public void markNotificationAsRead(Long notificationId, Long userId) {
        String readKey = "notification:read:" + userId;
        redisTemplate.opsForSet().add(readKey, notificationId);
        log.info("Notification {} marked as read for user {}", notificationId, userId);
    }
}
