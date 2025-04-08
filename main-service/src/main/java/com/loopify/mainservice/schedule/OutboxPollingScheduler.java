package com.loopify.mainservice.schedule;

import com.loopify.mainservice.notification.DistributedLockProvider;
import com.loopify.mainservice.notification.OutboxNotificationProcessor;
import com.loopify.mainservice.notification.RedisDistributedLockProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPollingScheduler {

    private final RedisDistributedLockProvider redisDistributedLockProvider;
    private final DistributedLockProvider lockProvider;
    private final OutboxNotificationProcessor outboxNotificationProcessor;

    private static final String OUTBOX_POLLER_LOCK_KEY = "outbox_poller_lock";
    private static final long LOCK_TIMEOUT_MS = 5000; // 5秒锁有效期

    @Scheduled(fixedDelay = 1000) // loop each second
    public void pollAndPublishOutboxMessages() {
        // 尝试获取分布式锁，防止多个实例同时轮询
        if (redisDistributedLockProvider.tryLock(OUTBOX_POLLER_LOCK_KEY, LOCK_TIMEOUT_MS)) {
            log.info("Lock successfully，begin to loop outbox notification...");
            try {
                outboxNotificationProcessor.processPendingMessages();
            } catch (Exception e) {
                log.error("An error occurred while processing the message: {}", e.getMessage(), e);
                // 出现异常不会阻止锁释放
            } finally {
                lockProvider.unlock(OUTBOX_POLLER_LOCK_KEY);
                log.info("unlock");
            }
        } else {
            log.debug("Unable to acquire lock, another instance may be processing");
        }
    }
}
