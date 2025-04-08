package com.loopify.mainservice.notification;

import com.loopify.mainservice.enums.NotificationStatus;
import com.loopify.mainservice.model.NotificationOutbox;
import com.loopify.mainservice.repository.notification.NotificationOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class OutboxNotificationProcessor {

    private final NotificationOutboxRepository notificationOutboxRepository;
    private final RabbitMQPublisher rabbitMQPublisher;

    private static final int BATCH_SIZE = 100; // 每批处理100条消息
    private static final int MAX_RETRIES = 5; // 最大重试次数



    @Transactional(readOnly = true) // 只读事务获取消息
    public void processPendingMessages() {
        // 优先处理重试次数较少的消息
        Pageable limit = PageRequest.of(0, BATCH_SIZE,
                Sort.by("retryCount").ascending().and(Sort.by("createdAt").ascending()));
        List<NotificationOutbox> notificationOutboxes = notificationOutboxRepository.findByStatus(NotificationStatus.PENDING, limit);

        if (notificationOutboxes.isEmpty()) {
            log.debug("No pending notifications found");
            return;
        }

        log.info("Found {} notifications", notificationOutboxes.size());

        for (NotificationOutbox notificationOutbox : notificationOutboxes) {
            try {
                // 尝试发布到RabbitMQ
                rabbitMQPublisher.publishNotification(notificationOutbox.getNotificationType(), notificationOutbox.getPayload(), notificationOutbox.getId());

                // 如果发布成功，更新消息状态
                updateMessageStatus(notificationOutbox.getId(), NotificationStatus.SENT);
                log.debug("Publish notification successfully，ID: {}", notificationOutbox.getId());

            } catch (Exception mqException) {
                log.error("Publish notification to RabbitMQ failed，ID: {}，error: {}",
                        notificationOutbox.getId(), mqException.getMessage(), mqException);
                handlePublishFailure(notificationOutbox, mqException);
            }
        }

        // 检测并处理长时间卡在PENDING状态的消息
        handleStuckMessages();
    }

    // 使用新事务更新消息状态
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateMessageStatus(Long notificationId, NotificationStatus newStatus) {
        try {
            int updatedRows = notificationOutboxRepository.updateStatusById(notificationId, newStatus);
            if (updatedRows == 0) {
                log.warn("Unable to update the status of message ID: {} to {}. The message may have been processed by another instance or deleted.", notificationId, newStatus);
            }
        } catch (Exception e) {
            log.error("An error occurred while updating the message status, ID: {}, New status: {}, Error: {}",
                    notificationId, newStatus, e.getMessage(), e);
            throw e; // Rethrow the exception to trigger a transaction rollback
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePublishFailure(NotificationOutbox notification, Exception exception) {
        try {
            // 递增重试计数
            int newRetryCount = notification.getRetryCount() + 1;

            if (newRetryCount > MAX_RETRIES) {
                // 超过最大重试次数，标记为失败
                updateMessageStatus(notification.getId(), NotificationStatus.FAILED);
                log.error("Message ID: {} Exceeded the maximum number of retries ({}) and has been marked as FAILED",
                        notification.getId(), MAX_RETRIES);
            } else {
                // 更新重试计数，保持PENDING状态供下次处理
                notificationOutboxRepository.incrementRetryCount(notification.getId());

                // 可以实现指数退避策略，根据重试次数增加延迟时间
                log.warn("Message ID: {} Will retry later, current retry count: {}", notification.getId(), newRetryCount);
            }
        } catch (Exception e) {
            log.error("An error occurred while processing a failed publish, message ID: {}, error: {}", notification.getId(), e.getMessage(), e);
            throw e; // Rethrow the exception to trigger a transaction rollback
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleStuckMessages() {
        // 查找超过一定时间（如30分钟）仍处于PENDING状态的消息
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(30);
        List<NotificationOutbox> stuckMessages = notificationOutboxRepository.findStuckMessages(
                NotificationStatus.PENDING, threshold, PageRequest.of(0, 50));

        if (!stuckMessages.isEmpty()) {
            log.warn("Found {} stuck messages (unprocessed for more than 30 minutes)", stuckMessages.size());

            for (NotificationOutbox message : stuckMessages) {
                if (message.getRetryCount() >= MAX_RETRIES) {
                    // 如果已经重试多次，标记为DEAD_LETTER
                    updateMessageStatus(message.getId(), NotificationStatus.DEAD_LETTER);
                    log.warn("Stuck message ID: {} has been marked as DEAD_LETTER", message.getId());
                } else {
                    // 重置消息状态，使其能被重新处理
                    log.info("Reset stuck message to retry, ID: {}", message.getId());
                    // 可选：增加重试计数
                    notificationOutboxRepository.incrementRetryCount(message.getId());
                }
            }
        }
    }

}
