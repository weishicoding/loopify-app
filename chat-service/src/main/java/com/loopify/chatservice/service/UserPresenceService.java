package com.loopify.chatservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UserPresenceService {

    private final StringRedisTemplate redisTemplate;

    @Value("${app.redis.presence-ttl-seconds}")
    private long presenceTtlSeconds;

    private String getPresenceKey(Long userId) {
        return "user:presence:" + userId;
    }

    private String getUnreadCountKey(Long userId) {
        return "user:unread_count:" + userId;
    }

    // Called when user connects via WebSocket
    public void markUserOnline(Long userId, String instanceId) {
        redisTemplate.opsForValue().set(getPresenceKey(userId), instanceId, presenceTtlSeconds, TimeUnit.SECONDS);
    }

    // Called on WebSocket heartbeat
    public boolean refreshPresence(Long userId) {
        return Boolean.TRUE.equals(redisTemplate.expire(getPresenceKey(userId), presenceTtlSeconds, TimeUnit.SECONDS));
    }

    // Called on WebSocket disconnect
    public void markUserOffline(Long userId) {
        redisTemplate.delete(getPresenceKey(userId));
    }

    // Check if user is online
    public boolean isUserOnline(Long userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(getPresenceKey(userId)));
    }

    // --- Unread Count Methods ---

    public void incrementUnreadCount(Long userId) {
        redisTemplate.opsForValue().increment(getUnreadCountKey(userId));
    }

    public long getUnreadCount(Long userId) {
        String countStr = redisTemplate.opsForValue().get(getUnreadCountKey(userId));
        return countStr != null ? Long.parseLong(countStr) : 0;
    }

    public void setUnreadCount(Long userId, long count) {
        if (count <= 0) {
            redisTemplate.delete(getUnreadCountKey(userId)); // Clear if zero or less
        } else {
            redisTemplate.opsForValue().set(getUnreadCountKey(userId), String.valueOf(count));
        }
    }
}
