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
        try {
            rabbitTemplate.convertAndSend(notificationExchange, followRoutingKey,
                    objectMapper.writeValueAsString(followNotification));
            log.info("Follow notification sent: User {} followed User {}", followNotification.getActionUserId(), followNotification.getTargetUserId());
            // Store in Redis for quick access
            String redisKey = "notification:user:" + followNotification.getTargetUserId();
            redisTemplate.opsForList().leftPush(redisKey, followNotification);
            redisTemplate.opsForList().trim(redisKey, 0, 99); // Keep only the 100 most recent
        } catch (Exception e) {
            log.error("Failed to send follow notification", e);
        }
    }

    @Override
    public void markNotificationAsRead(Long notificationId, Long userId) {
        String readKey = "notification:read:" + userId;
        redisTemplate.opsForSet().add(readKey, notificationId);
        log.info("Notification {} marked as read for user {}", notificationId, userId);
    }
}
