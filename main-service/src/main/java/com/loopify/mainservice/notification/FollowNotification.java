package com.loopify.mainservice.notification;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class FollowNotification extends BaseNotification {
    private Long actionUserId;
    private String actionUserNickname;
    private String actionUserAvatar;

    public FollowNotification(Long notificationId, Long targetUserId, Long actionUserId,
                              String actionUserNickname, String actionUserAvatar, LocalDateTime timestamp) {
        super(notificationId, "FOLLOW", targetUserId, timestamp);
        this.actionUserId = actionUserId;
        this.actionUserNickname = actionUserNickname;
        this.actionUserAvatar = actionUserAvatar;
    }
}
