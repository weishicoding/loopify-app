package com.loopify.mainservice.repository.notification;

import com.loopify.mainservice.model.NotificationOutbox;
import com.loopify.mainservice.enums.NotificationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, Long> {

    List<NotificationOutbox> findByStatus(NotificationStatus status, Pageable pageable);

    @Modifying
    @Query("UPDATE NotificationOutbox m SET m.status = :newStatus, m.processedAt = CURRENT_TIMESTAMP WHERE m.id = :id")
    int updateStatusById(Long id, NotificationStatus newStatus);

    //    Optional: Query for retry logic
    @Modifying
    @Query("UPDATE NotificationOutbox m SET m.retryCount = m.retryCount + 1 WHERE m.id = :id")
    int incrementRetryCount(Long id);

    @Query("SELECT m FROM NotificationOutbox m WHERE m.status = :status AND m.updatedAt < :threshold")
    List<NotificationOutbox> findStuckMessages(@Param("status") NotificationStatus status,
                                          @Param("threshold") LocalDateTime threshold,
                                          Pageable pageable);
}
