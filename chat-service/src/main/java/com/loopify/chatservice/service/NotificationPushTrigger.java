package com.loopify.chatservice.service;

import com.loopify.chatservice.notification.NotificationSavedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationPushTrigger {

    private final NotificationService notificationService; // Inject to call the async method

    // Listen for the event AFTER the transaction commits successfully
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationSaved(NotificationSavedEvent event) {
        log.debug("Transaction committed for notification {}. Triggering async push.", event.getNotification().getId());
        notificationService.triggerPushNotifications(event.getNotification());
    }
}
