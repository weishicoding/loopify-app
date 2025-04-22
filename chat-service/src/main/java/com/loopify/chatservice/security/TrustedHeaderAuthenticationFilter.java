package com.loopify.chatservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class TrustedHeaderAuthenticationFilter extends OncePerRequestFilter {
    private final CustomUserDetailsService customUserDetailsService;

    // Match header name used in Gateway AuthFilter
    private static final String USER_EMAIL_HEADER = "X-User-Email";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract user email from the trusted header
        String userEmail = request.getHeader(USER_EMAIL_HEADER);

        if (!StringUtils.hasText(userEmail)) {
            log.warn("Missing trusted header '{}' for path: {}", USER_EMAIL_HEADER, request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        log.debug("Attempting authentication based on header '{}': {}", USER_EMAIL_HEADER, userEmail);

        try {
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(userEmail);

            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());

            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authToken);
            log.debug("Successfully authenticated user '{}' from trusted header.", userEmail);

        } catch (UsernameNotFoundException e) {
            log.error("User '{}' from trusted header not found in local database.", userEmail);
            SecurityContextHolder.clearContext();
        } catch (Exception e) {
            log.error("Error processing trusted header authentication for user '{}'", userEmail, e);
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

}
