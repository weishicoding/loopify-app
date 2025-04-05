package com.loopify.mainservice.notification;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

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

    public CommentNotification(Long actionUserId, Long targetUserId, Long postId, String previewText) {
        super(null, actionUserId, targetUserId, "COMMENT", null);
        this.postId = postId;
        this.previewText = previewText;
    }
}
