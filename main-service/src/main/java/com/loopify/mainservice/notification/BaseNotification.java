package com.loopify.mainservice.notification;

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
})
public abstract class BaseNotification implements Serializable {
    private Long notificationId;
    private String type;
    private Long targetUserId;
    private LocalDateTime timestamp;
}
