package com.loopify.chatservice.model;


import com.loopify.chatservice.enums.CommentType;
import com.loopify.chatservice.enums.NotificationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private Long actionUserId;  // Who triggered the notification

    private Long targetUserId;  // Who receives the notification

    private String actionUserName;

    private String actionUserAvatar;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    private String content;

    private CommentType relatedEntityType;  // POST, COMMENT, etc.

    private Long relatedEntityId;  // ID of the related entity

    @Column(name = "is_read")
    private boolean isRead;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        isRead = false;
    }

    @PreUpdate
    public void onUpdate () {
        updatedAt = LocalDateTime.now();
    }
}
