package com.loopify.chatservice.notification;

import com.loopify.chatservice.model.Notification;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NotificationSavedEvent {

    private final Object source;

    private final Notification notification;
}
