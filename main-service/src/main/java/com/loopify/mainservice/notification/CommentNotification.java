package com.loopify.mainservice.notification;

import com.loopify.mainservice.enums.CommentType;
import com.loopify.mainservice.enums.NotificationType;
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
public class CommentNotification extends BaseNotification {
    private Long relatedId;
    private CommentType relatedType;
    private String context;

    public CommentNotification(Long notificationId, Long actionUserId, Long targetUserId, Long relatedId, CommentType relatedType, String actionUserNickname, String actionUserAvatar, String context) {
        super(notificationId, actionUserId, targetUserId, NotificationType.COMMENT, LocalDateTime.now(), actionUserNickname, actionUserAvatar);
        this.relatedId = relatedId;
        this.relatedType = relatedType;
        this.context = context;
    }
}
