package com.loopify.chatservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationWebSocketHandler extends TextWebSocketHandler {


    private final Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final UserPresenceService userPresenceService;
    private final ObjectMapper objectMapper;

    @Value("${spring.application.name}:${random.uuid}") // Unique ID for this instance
    private String instanceId;

    @Value("${app.websocket.heartbeat-interval-ms}")
    private long heartbeatIntervalMs; // Used for presence TTL calculation

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        Long userId = getUserIdFromSession(session); // Implement this based on your auth
        if (userId != null) {
            log.info("WebSocket connection established for user: {}, session: {}", userId, session.getId());
            sessions.put(userId, session);
            userPresenceService.markUserOnline(userId, instanceId);
        } else {
            log.warn("WebSocket connection rejected: Unable to authenticate user. Session: {}", session.getId());
            session.close(CloseStatus.POLICY_VIOLATION);
        }
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        Long userId = getUserIdFromSession(session);
        if (userId == null) return; // Should not happen if connection established correctly

        String payload = message.getPayload();
        log.debug("Received WebSocket message from user {}: {}", userId, payload);

        // Simple heartbeat handling
        if ("PING".equalsIgnoreCase(payload)) {
            boolean refreshed = userPresenceService.refreshPresence(userId);
            if (!refreshed) {
                // Maybe the key expired just before refresh, re-mark online
                userPresenceService.markUserOnline(userId, instanceId);
            }
            session.sendMessage(new TextMessage("PONG")); // Respond to heartbeat
        } else {
            // Handle other client messages if needed
        }
    }

    @Override
    public void handleTransportError(@NonNull WebSocketSession session, @NonNull Throwable exception) throws Exception {
        log.error("WebSocket transport error for session {}: {}", session.getId(), exception.getMessage());
        // May need cleanup here too
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        Long userId = getUserIdFromSession(session);
        if (userId != null) {
            log.info("WebSocket connection closed for user: {}, session: {}, status: {}", userId, session.getId(), status);
            sessions.remove(userId);
            userPresenceService.markUserOffline(userId);
        }
    }

    // Method to send message to a specific user
    public boolean sendMessageToUser(Long userId, Object messagePayload) {
        WebSocketSession session = sessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                String jsonMessage = objectMapper.writeValueAsString(messagePayload);
                session.sendMessage(new TextMessage(jsonMessage));
                log.debug("Sent WebSocket message to user {}: {}", userId, jsonMessage);
                return true;
            } catch (IOException e) {
                log.error("Failed to send WebSocket message to user {}: {}", userId, e.getMessage());
                return false;
            }
        }
        log.warn("Could not send WebSocket message: No open session found for user {}", userId);
        return false;
    }


    private Long getUserIdFromSession(WebSocketSession session) {
        try {
            String query = Objects.requireNonNull(session.getUri()).getQuery(); // e.g., "token=...&userId=123"
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] pair = param.split("=");
                    if (pair.length == 2 && "userId".equals(pair[0])) {
                        return Long.parseLong(pair[1]);
                    }
                }
            }
        } catch (Exception e) { log.error("Error parsing userId from session URI", e); }
        return null;
    }
}
