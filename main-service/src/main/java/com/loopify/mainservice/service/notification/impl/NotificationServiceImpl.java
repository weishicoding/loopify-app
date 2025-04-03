package com.loopify.mainservice.service.notification.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopify.mainservice.notification.FollowNotification;
import com.loopify.mainservice.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;


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
    public void sendFollowNotification(FollowNotification followNotification) {
        String redisKey = "notification:user:" + followNotification.getTargetUserId();
        try {
            // 先存入 Redis
            redisTemplate.opsForList().leftPush(redisKey, followNotification);
            redisTemplate.opsForList().trim(redisKey, 0, 99); // 保留最近 100 条

            // 再发送到 RabbitMQ
            rabbitTemplate.convertAndSend(notificationExchange, followRoutingKey,
                    objectMapper.writeValueAsString(followNotification));
            log.info("Follow notification sent: User {} followed User {}",
                    followNotification.getActionUserId(), followNotification.getTargetUserId());
        } catch (Exception e) {
            // 如果 RabbitMQ 发送失败，回滚 Redis 操作
            redisTemplate.opsForList().remove(redisKey, 1, followNotification);
            log.error("Failed to send follow notification, rolled back Redis operation", e);
        }
    }

    @Override
    public void markNotificationAsRead(Long notificationId, Long userId) {
        String readKey = "notification:read:" + userId;
        redisTemplate.opsForSet().add(readKey, notificationId);
        log.info("Notification {} marked as read for user {}", notificationId, userId);
    }
}
