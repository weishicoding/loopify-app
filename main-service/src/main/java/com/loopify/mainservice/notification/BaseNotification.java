package com.loopify.mainservice.notification;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.loopify.mainservice.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = FollowNotification.class, name = "FOLLOW"),
        @JsonSubTypes.Type(value = CommentNotification.class, name = "COMMENT")
})
public class BaseNotification implements Serializable {
    private Long notificationId;
    private Long actionUserId;
    private Long targetUserId;
    private NotificationType type;
    private LocalDateTime timestamp;
    private String actionUserNickname;
    private String actionUserAvatar;
}
