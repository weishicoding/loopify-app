package com.loopify.chatservice.comsumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopify.chatservice.notification.FollowNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationConsumer {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @RabbitListener(queues = "${rabbitmq.queue.follow-notifications}")
    public void handleFollowNotification(String notificationJson) {
        try {
            FollowNotification notification = objectMapper.readValue(notificationJson, FollowNotification.class);

            String notificationId = String.valueOf(notification.getNotificationId());
            String processedKey = "processed:notifications:" + notification.getTargetUserId();

            // check if notifications was dealt
            if (Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(processedKey, notificationId))) {
                log.info("Notification {} already processed, skipping", notificationId);
                return;
            }

            String userId = String.valueOf(notification.getTargetUserId());
            // check if user is online
            if (Boolean.TRUE.equals(redisTemplate.opsForSet().isMember("online:users", userId))) {
                messagingTemplate.convertAndSendToUser(
                        userId,
                        "/topic/notifications",
                        notification
                );
                log.info("{} notification sent to WebSocket for user {}", notification.getType(), userId);
            } else {
                log.info("User {} is offline, notification not sent via WebSocket", userId);
            }

            // mark as it was dealt
            redisTemplate.opsForSet().add(processedKey, notificationId);
        } catch (Exception e) {
            log.error("Error processing notification", e);
        }
    }
}
