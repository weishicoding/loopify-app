package com.loopify.chatservice.service;

import com.loopify.chatservice.model.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class MobilePushService {

    // Inject FCM service
    private final FirebaseMessaging firebaseMessaging;

    // Optional: Inject APNs service if you're implementing it separately
    // private final ApnsClient apnsClient;

    // Inject user device repository or service
    private final UserDeviceRepository userDeviceRepository;

    @Autowired
    public MobilePushService(FirebaseMessaging firebaseMessaging,
                             UserDeviceRepository userDeviceRepository) {
        this.firebaseMessaging = firebaseMessaging;
        this.userDeviceRepository = userDeviceRepository;
    }

    public void sendNotification(Long userId, Notification notification) {
        // 1. Retrieve the user's device tokens
        List<UserDevice> userDevices = userDeviceRepository.findByUserId(userId);

        if (userDevices.isEmpty()) {
            log.debug("No registered devices found for user {}", userId);
            return;
        }

        // 2. Prepare notification content based on notification type
        String title = getNotificationTitle(notification);
        String body = getNotificationBody(notification);

        // 3. Send to each device with appropriate platform
        for (UserDevice device : userDevices) {
            try {
                if (device.getPlatform() == Platform.ANDROID || device.getPlatform() == Platform.IOS) {
                    // FCM supports both Android and iOS
                    sendFcmNotification(device.getToken(), title, body, notification);
                }

                // If you need separate APNs implementation for iOS:
                // if (device.getPlatform() == Platform.IOS) {
                //     sendApnsNotification(device.getToken(), title, body, notification);
                // }

            } catch (Exception e) {
                log.error("Failed to send notification to device {} of user {}: {}",
                        device.getId(), userId, e.getMessage(), e);
                // Continue with other devices
            }
        }
    }

    private void sendFcmNotification(String token, String title, String body, Notification notification) {
        try {
            // Create notification message
            Map<String, String> data = new HashMap<>();
            data.put("notificationId", notification.getId().toString());
            data.put("type", notification.getType().toString());
            data.put("timestamp", String.valueOf(System.currentTimeMillis()));

            // Build FCM message
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(FirebaseMessaging.Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putAllData(data)
                    .build();

            // Send message
            String messageId = firebaseMessaging.send(message);
            log.info("FCM notification sent successfully to token {}, message ID: {}", token, messageId);

        } catch (FirebaseMessagingException e) {
            // Handle specific FCM errors
            if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                // Token is no longer valid - should be removed from your database
                log.warn("Device token {} is no longer registered, should be removed", token);
                // userDeviceRepository.deleteByToken(token);
            }
            throw new RuntimeException("FCM send failed: " + e.getMessage(), e);
        }
    }

    // Helper methods to generate appropriate notification content
    private String getNotificationTitle(Notification notification) {
        switch (notification.getType()) {
            case FOLLOW:
                return "New Follower";
            case COMMENT:
                return "New Comment";
            case LIKE:
                return "New Like";
            default:
                return "New Notification";
        }
    }

    private String getNotificationBody(Notification notification) {
        // Create appropriate message based on notification content
        return notification.getContent();
    }
}
