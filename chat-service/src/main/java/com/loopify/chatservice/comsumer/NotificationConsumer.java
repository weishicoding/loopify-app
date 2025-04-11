package com.loopify.chatservice.comsumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopify.chatservice.enums.NotificationType;
import com.loopify.chatservice.model.Notification;
import com.loopify.chatservice.notification.CommentNotification;
import com.loopify.chatservice.notification.FollowNotification;
import com.loopify.chatservice.notification.NotificationSavedEvent;
import com.loopify.chatservice.service.IdempotencyService;
import com.loopify.chatservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationConsumer {

    private final ObjectMapper objectMapper;
    private final IdempotencyService idempotencyService;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher; // To trigger async push AFTER TX commit

    @RabbitListener(queues = { // Use curly braces for a String array
            "${rabbitmq.queue.follow-notifications}",
            "${rabbitmq.queue.comment-notifications}"
    })
    @Transactional // Start transaction here for DB writes + idempotency check
    public void handleFollowNotification(String messagePayload,
                                         @Header(AmqpHeaders.MESSAGE_ID) String messageId,
                                         @Header(value = "X-Event-Type", required = false, defaultValue = "UNKNOWN") NotificationType messageType) {

        log.debug("Received message ID: {}, Event type: {}", messageId, messageType);

        // Step 1: Idempotency check
        if (idempotencyService.isProcessed(messageId)) {
            log.info("Message {} already processed, skipping", messageId);
            return; // Skip processing, message will be ACKed
        }

        try {
            // Step 2: Process and persist notification
            Notification notification = processEvent(messagePayload, messageType);

            if (notification != null) {
                // Step 3: Save notification to database
                Notification savedNotification = notificationService.saveNotification(notification);

                // Step 4: Mark message as processed (within same transaction)
                idempotencyService.markAsProcessed(messageId);

                // Step 5: Trigger push notifications after transaction commits
                if (savedNotification != null) {
                    eventPublisher.publishEvent(new NotificationSavedEvent(this, savedNotification));
                    log.info("Successfully processed notification from message {}, notification ID: {}",
                            messageId, savedNotification.getId());
                }
            } else {
                log.warn("Event processing yielded no notification, marking as processed: {}", messageId);
                idempotencyService.markAsProcessed(messageId);
            }

        } catch (Exception e) {
            log.error("Failed to process message {}: {}", messageId, e.getMessage(), e);
            // Re-throw to trigger NACK and DLQ handling
            throw new RuntimeException("Failed to process message: " + e.getMessage(), e);
        }
    }

    private Notification processEvent(String payload, NotificationType eventType) {
        try {
            if (eventType == NotificationType.COMMENT) {
                CommentNotification commentNotification = objectMapper.readValue(payload, CommentNotification.class);
                return Notification.builder()
                        .actionUserId(commentNotification.getActionUserId())
                        .actionUserAvatar(commentNotification.getActionUserAvatar())
                        .actionUserName(commentNotification.getActionUserNickname())
                        .targetUserId(commentNotification.getTargetUserId())
                        .relatedEntityType(commentNotification.getRelatedType())
                        .relatedEntityId(commentNotification.getRelatedId())
                        .type(commentNotification.getType())
                        .content(commentNotification.getActionUserNickname() + " commented on your post: " +
                                truncateIfNeeded(commentNotification.getContext()))
                        .build();
            } else if (eventType == NotificationType.FOLLOW) {
                FollowNotification followNotification = objectMapper.readValue(payload, FollowNotification.class);
                return Notification.builder()
                        .actionUserId(followNotification.getActionUserId())
                        .actionUserAvatar(followNotification.getActionUserAvatar())
                        .actionUserName(followNotification.getActionUserNickname())
                        .targetUserId(followNotification.getTargetUserId())
                        .type(followNotification.getType())
                        .content(followNotification.getActionUserNickname() + " started following you")
                        .build();
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize event payload: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Invalid event payload", e);

        }
        return new Notification();
    }

    private String truncateIfNeeded(String text) {
        if (text == null) return "";
        return text.length() <= 50 ? text : text.substring(0, 50) + "...";
    }
}
