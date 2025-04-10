package com.loopify.chatservice.config;

import com.loopify.chatservice.service.NotificationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final NotificationWebSocketHandler notificationWebSocketHandler;

//    @Override
//    public void configureMessageBroker(MessageBrokerRegistry config) {
//        config.enableSimpleBroker("/topic");
//        config.setApplicationDestinationPrefixes("/app");
//    }

//    @Override
//    public void registerWebSocketHandlers(StompEndpointRegistry registry) {
//        // ws://localhost/api/chat
//        registry.addEndpoint( "/chat").setAllowedOrigins("*").withSockJS();
//    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(notificationWebSocketHandler, "/ws/notifications").setAllowedOrigins("*").withSockJS();
    }
}
