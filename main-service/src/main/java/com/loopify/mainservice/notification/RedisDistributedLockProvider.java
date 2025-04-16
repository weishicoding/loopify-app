package com.loopify.mainservice.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@RequiredArgsConstructor
public class RedisDistributedLockProvider implements DistributedLockProvider{

    private final StringRedisTemplate redisTemplate;
    private final String instanceId = UUID.randomUUID().toString();

    // Lua脚本，用于原子性地检查并删除锁
    private static final String UNLOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "    return redis.call('del', KEYS[1]) " +
                    "else " +
                    "    return 0 " +
                    "end";

    @Override
    public boolean tryLock(String lockKey, long timeoutMillis) {
        try {
            Boolean success = redisTemplate.opsForValue().setIfAbsent(
                    lockKey, instanceId, timeoutMillis, TimeUnit.MILLISECONDS);
            return Boolean.TRUE.equals(success);
        } catch (Exception e) {
            log.error("An error occurred while acquiring a distributed lock: {}", e.getMessage(), e);
            return false; // 出错时视为获取锁失败
        }
    }

    @Override
    public void unlock(String lockKey) {
        try {
            // 使用Lua脚本确保原子性操作
            RedisScript<Long> script = RedisScript.of(UNLOCK_SCRIPT, Long.class);
            Long result = redisTemplate.execute(script, Collections.singletonList(lockKey), instanceId);

            if (result == null || result == 0) {
                log.warn("Unable to release lock {}, may have expired or is held by another instance", lockKey);
            }
        } catch (Exception e) {
            log.error("An error occurred while releasing the distributed lock: {}", e.getMessage(), e);
        }
    }
}
