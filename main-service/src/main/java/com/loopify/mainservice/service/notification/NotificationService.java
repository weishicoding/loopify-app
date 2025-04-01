package com.loopify.mainservice.service.notification;

import com.loopify.mainservice.model.user.User;
import com.loopify.mainservice.notification.FollowNotification;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

public interface NotificationService {
    // Existing method
    void sendFollowNotification(FollowNotification followNotification);

    void markNotificationAsRead(Long notificationId, Long userId);
}
