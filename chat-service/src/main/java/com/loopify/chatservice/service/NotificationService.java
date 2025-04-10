package com.loopify.chatservice.service;

import com.loopify.chatservice.model.Notification;
import com.loopify.chatservice.notification.WsNotificationPayload;
import com.loopify.chatservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserPresenceService userPresenceService;
    private final MobilePushService mobilePushService;
    private final NotificationWebSocketHandler notificationWebSocketHandler;


    @Transactional
    public Notification saveNotification(Notification notification) {
        Notification savedNotification = notificationRepository.save(notification);
        log.info("Saved notification {} for user {}", savedNotification.getId(), savedNotification.getId());

        // Increment unread count in Redis (within the same transaction)
        userPresenceService.incrementUnreadCount(savedNotification.getId());

        return savedNotification;
    }

    @Async("notificationPushExecutor")
    public void triggerPushNotifications(Notification notification) {
        if (notification == null) return;
        Long userId = notification.getId();
        // Check online status
        boolean isOnline = userPresenceService.isUserOnline(userId);

        if (isOnline) {
            log.info("User {} is ONLINE. Attempting WebSocket push.", userId);
            // Prepare payload for WebSocket (maybe simpler than full Notification)
            WsNotificationPayload wsPayload = WsNotificationPayload.fromNotification(notification);
            boolean pushedViaWebSocket = notificationWebSocketHandler.sendMessageToUser(userId, wsPayload);

            if (!pushedViaWebSocket) {
                log.warn("WebSocket push failed for online user {}. Consider mobile push fallback.", userId);
            }
        } else {
            log.info("User {} is OFFLINE. Sending mobile push.", userId);
            mobilePushService.sendNotification(userId, notification);
        }
    }
}
