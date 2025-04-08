package com.loopify.mainservice.notification;

public interface DistributedLockProvider {

    /**
     * Attempts to acquire a lock for the specified key
     *
     * @param lockKey Unique identifier for the lock
     * @param timeoutMillis Lock timeout duration in milliseconds
     * @return true if lock was acquired successfully, false otherwise
     */
    boolean tryLock(String lockKey, long timeoutMillis);

    /**
     * Releases the lock for the specified key
     *
     * @param lockKey Unique identifier for the lock
     */
    void unlock(String lockKey);
}
