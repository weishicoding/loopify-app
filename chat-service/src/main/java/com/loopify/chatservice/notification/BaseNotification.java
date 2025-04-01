package com.loopify.chatservice.notification;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = FollowNotification.class, name = "FOLLOW")
        // 后续可添加其他类型，例如 @JsonSubTypes.Type(value = LikeNotification.class, name = "LIKE")
})
public abstract class BaseNotification implements Serializable {
    private Long notificationId;
    private String type;
    private Long targetUserId;
    private LocalDateTime timestamp;
}
