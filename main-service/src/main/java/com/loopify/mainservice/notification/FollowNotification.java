package com.loopify.mainservice.notification;

import com.loopify.mainservice.enums.NotificationType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
public class FollowNotification extends BaseNotification {

    public FollowNotification(Long notificationId, Long actionUserId, Long targetUserId, String actionUserNickname, String actionUserAvatar) {
        super(notificationId, actionUserId, targetUserId, NotificationType.FOLLOW, LocalDateTime.now(), actionUserNickname, actionUserAvatar);
    }
}
