package com.loopify.mainservice.service.notification;

import com.loopify.mainservice.model.user.User;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

public interface NotificationService {
    // Existing method
    void sendFollowNotification(User follower, User following);

    void markNotificationAsRead(Long notificationId, Long userId);

    // todo: New methods for other notification types

    // Base notification class
    @Data
    abstract class BaseNotification {
        private Long notificationId;
        private String type;
        private Long targetUserId;
        private LocalDateTime timestamp;
    }

    // Existing notification
    @EqualsAndHashCode(callSuper = true)
    @Data
    class FollowNotification extends BaseNotification {
        private Long actionUserId;
        private String actionUserNickname;
        private String actionUserAvatar;

        public FollowNotification() {
            setType("FOLLOW");
        }
    }

    // todo: New notification same types in future
}
