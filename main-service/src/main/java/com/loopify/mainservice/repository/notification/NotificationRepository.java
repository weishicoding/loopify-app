package com.loopify.mainservice.repository.notification;

import com.loopify.mainservice.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByTargetUserIdOrderByCreatedAtDesc(Long targetUserId, Pageable pageable);

    List<Notification> findByTargetUserIdAndReadIsFalseOrderByCreatedAtDesc(Long targetUserId);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.read = true WHERE n.id = :notificationId AND n.targetUserId = :userId")
    int markAsRead(Long notificationId, Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.read = true WHERE n.targetUserId = :userId AND n.read = false")
    int markAllAsRead(Long userId);

    long countByTargetUserIdAndReadIsFalse(Long targetUserId);

    long countByTargetUserId(Long targetUserId);

    List<Notification> findByTargetUserIdAndReadIsFalseAndIdNotInOrderByCreatedAtDesc(
            Long targetUserId, Set<Long> excludeIds, Pageable pageable);
}
