package com.loopify.mainservice.notification;

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

    public FollowNotification(Long actionUserId, Long targetUserId) {
        super(null, actionUserId, targetUserId, "FOLLOW", null);
    }
}
