package com.loopify.mainservice.model;

import com.loopify.mainservice.enums.NotificationType;
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

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    private String content;     // Content varies by notification type
    private boolean read;       // Read status
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        read = false;
    }
}
