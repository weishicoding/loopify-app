package com.loopify.chatservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopify.chatservice.enums.NotificationType;
import com.loopify.chatservice.model.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;
    private final NotificationWebSocketHandler webSocketHandler;
    private final MobilePushService mobilePushService;
    private final UserPresenceService userPresenceService;


    @Transactional
    public Notification createAndProcessNotification(NotificationType eventType, String messagePayload, String messageId) {


        Notification notification = new Notification();
        if ("USER_FOLLOWED".equals(eventType)) {
            notification.setType("FOLLOW");
            notification.setContent("Someone followed you!"); // Placeholder
        } else if ("NEW_COMMENT".equals(eventType)) {
            // ... parse NewCommentEvent ...
            notification.setType("COMMENT");
            notification.setContent("New comment on your post!"); // Placeholder
        } else {
            log.warn("Unsupported event type: {}", eventType);
            return null;
        }
        // Mock for example:
        notification.setUserId(1L); // Hardcode recipient for testing
        notification.setSenderId(2L); // Hardcode sender for testing


        // 2. Save Notification
        Notification savedNotification = notificationRepository.save(notification);
        log.info("Saved notification {} for user {}", savedNotification.getId(), savedNotification.getUserId());

        // 3. Increment Unread Count (after successful save)
        userPresenceService.incrementUnreadCount(savedNotification.getUserId());

        // IdempotencyService.markAsProcessed(messageId) should be called by consumer *within* the same TX

        return savedNotification; // Return saved entity for push triggering

        // } catch (Exception e) {
        //     log.error("Error creating notification from event: {}", eventType, e);
        //     throw new RuntimeException("Failed to create notification", e); // Propagate to NACK message
        // }
    }

    // Trigger Push Notifications (Called Async After Commit)
    @Async // Run in a separate thread
    public void triggerPushNotifications(Notification notification) {
        if (notification == null) return;
        Long recipientId = notification.getUserId();

        log.debug("Triggering push for notification {} to user {}", notification.getId(), recipientId);

        // Check online status
        boolean isOnline = userPresenceService.isUserOnline(recipientId);

        if (isOnline) {
            log.info("User {} is ONLINE. Attempting WebSocket push.", recipientId);
            // Prepare payload for WebSocket (maybe simpler than full Notification)
            WsNotificationPayload wsPayload = WsNotificationPayload.fromNotification(notification);
            boolean pushedViaWebSocket = webSocketHandler.sendMessageToUser(recipientId, wsPayload);

            // Optional: If WS push fails despite being marked online, fallback to mobile push?
            if (!pushedViaWebSocket) {
                log.warn("WebSocket push failed for online user {}. Consider mobile push fallback.", recipientId);
                // mobilePushService.sendNotification(recipientId, notification); // Decide on fallback strategy
            }
        } else {
            log.info("User {} is OFFLINE. Sending mobile push.", recipientId);
            mobilePushService.sendNotification(recipientId, notification);
        }
    }

    // --- Mark Read Logic ---
    @Transactional
    public void markNotificationsAsRead(Long userId, List<Long> notificationIds) {
        if (notificationIds == null || notificationIds.isEmpty()) return;
        int updatedCount = notificationRepository.markAsReadByIdsAndUserId(notificationIds, userId, LocalDateTime.now());
        if (updatedCount > 0) {
            recalculateAndSetUnreadCount(userId);
            // Optional: Notify client via WebSocket that these are read (for other open tabs/devices)
            // webSocketHandler.sendMessageToUser(userId, new WsReadUpdatePayload(notificationIds, ...));
        }
        log.info("Marked {} notifications as read for user {}", updatedCount, userId);
    }

    @Transactional
    public void markAllNotificationsAsRead(Long userId) {
        int updatedCount = notificationRepository.markAllAsReadByUserId(userId, LocalDateTime.now());
        if (updatedCount > 0) {
            userPresenceService.setUnreadCount(userId, 0); // Reset count to zero
            // Optional: Notify client via WebSocket
            // webSocketHandler.sendMessageToUser(userId, new WsReadAllUpdatePayload(...));
        }
        log.info("Marked all {} unread notifications as read for user {}", updatedCount, userId);
    }

    // Helper to recalculate count after marking read
    private void recalculateAndSetUnreadCount(Long userId) {
        long newUnreadCount = notificationRepository.countByUserIdAndIsReadFalse(userId);
        userPresenceService.setUnreadCount(userId, newUnreadCount);
        log.debug("Recalculated unread count for user {}: {}", userId, newUnreadCount);
    }
}
