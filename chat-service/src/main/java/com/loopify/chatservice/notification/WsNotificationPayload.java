package com.loopify.chatservice.notification;

import com.loopify.chatservice.model.Notification;

import java.time.LocalDateTime;

public class WsNotificationPayload {

    public String type = "NEW_NOTIFICATION"; // To help client distinguish message types
    public Long id;
    public String notificationType;
    public String content;
    public Long senderId;
    // Add other fields needed by the frontend in real-time
    public LocalDateTime createdAt;

    public static WsNotificationPayload fromNotification(Notification n) {
        WsNotificationPayload p = new WsNotificationPayload();
        p.id = n.getId();
        p.notificationType = n.getType().name();
        p.content = n.getContent();
        p.senderId = n.getActionUserId();
        p.createdAt = n.getCreatedAt();
        return p;
    }
}
