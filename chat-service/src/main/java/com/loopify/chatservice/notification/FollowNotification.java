package com.loopify.chatservice.notification;

import com.loopify.chatservice.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FollowNotification extends BaseNotification {
    private String actionUserNickname;
    private String actionUserAvatar;

    public FollowNotification(Long notificationId, Long actionUserId, Long targetUserId, String actionUserNickname, String actionUserAvatar) {
        super(notificationId, actionUserId, targetUserId, NotificationType.FOLLOW, LocalDateTime.now());
        this.actionUserNickname = actionUserNickname;
        this.actionUserAvatar = actionUserAvatar;
    }
}
