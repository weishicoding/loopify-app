package com.loopify.mainservice.service.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    public void consumeFollowNotification(String notificationJson) {
        processNotification(notificationJson, NotificationService.FollowNotification.class);
    }

    private <T extends NotificationService.BaseNotification> void processNotification(String notificationJson, Class<T> notificationType) {
        try {
            T notification = objectMapper.readValue(notificationJson, notificationType);

            // Send WebSocket notification to the target user
            String destination = "/topic/notifications/" + notification.getTargetUserId();
            messagingTemplate.convertAndSend(destination, notification);

            log.info("{} notification sent to WebSocket: {}",
                    notification.getType(), destination);
        } catch (Exception e) {
            log.error("Error processing notification", e);
        }
    }
}
