package com.loopify.chatservice.model;

import com.loopify.chatservice.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationOutboxEvent {

    private Long id;

    private NotificationType notificationType;

    private String payload;
}
