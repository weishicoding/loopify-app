package com.loopify.mainservice.model;

import com.loopify.mainservice.enums.NotificationStatus;
import com.loopify.mainservice.enums.NotificationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_outbox", indexes = {
        @Index(name = "inx_notification_outbox_status", columnList = "status")
})
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class NotificationOutbox {

    @Id
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType notificationType;

    @Column(columnDefinition = "TEXT", nullable = false) // Or JSONB if DB supports it
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime processedAt;

    @Column(nullable = false)
    private int retryCount = 0;

    // Optional: for optimistic locking
    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
         createdAt = LocalDateTime.now();
         status = NotificationStatus.PENDING;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
