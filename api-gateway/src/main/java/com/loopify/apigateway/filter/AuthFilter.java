package com.loopify.apigateway.filter;

import com.loopify.apigateway.security.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthFilter implements GlobalFilter, Ordered {

    private final JwtService jwtService;

    @Value("${app.auth.skip-paths}")
    private List<String> skipPaths;

    private static final String USER_EMAIL_HEADER = "X-User-Email";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 1. Skip authentication for public paths
        if (isSkipped(path)) {
            log.debug("Skipping auth for path: {}", path);
            return chain.filter(exchange);
        }

        // 2. Get Authorization header
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for path: {}", path);
            return handleUnauthorized(exchange, "Authorization header is missing or invalid");
        }

        // 3. Extract token
        String jwt = authHeader.substring(7);

        try {
            // 4. Validate token and extract email (or user ID)
            String email = jwtService.extractUsername(jwt);

            if (email != null && !jwtService.isTokenExpired(jwt)) {
                log.debug("JWT validated for user: {}, path: {}", email, path);

                // 5. Add user email header to the request before forwarding
                ServerHttpRequest modifiedRequest = request.mutate()
                        .header(USER_EMAIL_HEADER, email)
                        .build();

                // 6. Continue the filter chain with the modified request
                return chain.filter(exchange.mutate().request(modifiedRequest).build());
            } else {
                log.warn("JWT validation failed or email null for path: {}", path);
                return handleUnauthorized(exchange, "Invalid or expired token");
            }
        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT received for path: {}: {}", path, e.getMessage());
            return handleUnauthorized(exchange, "Token expired");
        } catch (JwtException | IllegalArgumentException e) {
            log.error("JWT parsing/validation error for path: {}: {}", path, e.getMessage());
            return handleUnauthorized(exchange, "Invalid token");
        } catch (Exception e) {
            log.error("Unexpected error during auth filter execution for path: {}: {}", path, e.getMessage(), e);
            return handleServerError(exchange);
        }
    }

    private boolean isSkipped(String path) {
        return skipPaths.stream().anyMatch(p -> path.startsWith(p.replace("/**", "")));
    }

    private Mono<Void> handleUnauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return response.setComplete();
    }

    private Mono<Void> handleServerError(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        return response.setComplete();
    }


    @Override
    public int getOrder() {
        return -100; // Run before most other filters, especially routing
    }
}
