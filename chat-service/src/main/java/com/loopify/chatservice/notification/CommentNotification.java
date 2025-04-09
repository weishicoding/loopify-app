package com.loopify.chatservice.notification;

import com.loopify.chatservice.enums.CommentType;
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
