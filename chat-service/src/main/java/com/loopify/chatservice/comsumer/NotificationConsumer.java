package com.loopify.chatservice.comsumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopify.chatservice.notification.FollowNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationConsumer {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "${rabbitmq.queue.follow-notifications}")
    public void handleFollowNotification(String notificationJson) {
        try {
            FollowNotification notification = objectMapper.readValue(notificationJson, FollowNotification.class);
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(notification.getTargetUserId()),
                    "/topic/notifications",
                    notification
            );
            log.info("{} notification sent to WebSocket for user {}",
                    notification.getType(), notification.getTargetUserId());
        } catch (Exception e) {
            log.error("Error processing notification", e);
        }
    }
}
