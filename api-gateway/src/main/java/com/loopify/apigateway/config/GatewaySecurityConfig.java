package com.loopify.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity // Enable WebFlux Security
public class GatewaySecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
                // --- Disable CSRF for the Gateway ---
                .csrf(ServerHttpSecurity.CsrfSpec::disable) // Standard way for WebFlux
                // --- Disable other defaults if needed ---
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                // --- Configure Authorization (Gateway usually permits all routed paths) ---
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/**").permitAll() // Allow all requests to pass through to be handled by routes/filters
                        .anyExchange().authenticated() // Should not be reached if pathMatchers("/**") is used
                );

        return http.build();
    }
}
