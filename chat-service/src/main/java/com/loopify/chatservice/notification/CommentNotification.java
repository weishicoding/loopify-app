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
public class CommentNotification extends BaseNotification {
    private String actionUserName;
    private String actionUserAvatar;
    private Long postId;
    private String previewText;

    public CommentNotification(Long notificationId, Long actionUserId, Long targetUserId, Long postId, String previewText) {
        super(notificationId, actionUserId, targetUserId, NotificationType.COMMENT, LocalDateTime.now());
        this.postId = postId;
        this.previewText = previewText;
    }
}
