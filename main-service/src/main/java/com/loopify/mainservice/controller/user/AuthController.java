package com.loopify.mainservice.controller.user;

import com.loopify.mainservice.dto.user.LoginRequest;
import com.loopify.mainservice.dto.user.RegisterRequest;
import com.loopify.mainservice.dto.user.UserDto;
import com.loopify.mainservice.model.user.RefreshToken;
import com.loopify.mainservice.model.user.User;
import com.loopify.mainservice.security.JwtService;
import com.loopify.mainservice.security.RefreshTokenService;
import com.loopify.mainservice.security.CustomUserDetailsService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userService;
    private final PasswordEncoder passwordEncoder;

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final String REFRESH_TOKEN_PATH = "/api/v1/auth";

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        // Check if email already exists
        if (userService.existsByEmail(request.getEmail())) {
            log.warn("Registration attempt with existing email: {}", request.getEmail());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Email already in use"));
        }

        // Password validation moved to the RegisterRequest DTO using Bean Validation
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Passwords do not match"));
        }

        // Create new user
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(generateDefaultNickname())
                .isActive(true)
                .build();

        User savedUser = userService.save(user);
        log.info("User registered successfully with ID: {}", savedUser.getId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "User registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        try {
            // Authenticate the user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User user = userService.findByEmail(userDetails.getUsername());

            // Generate JWT token
            String accessToken = jwtService.generateToken(userDetails);

            // Generate refresh token and save it
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

            // Set refresh token in HTTP-only cookie
            addRefreshTokenCookie(response, refreshToken.getToken());

            // Update last seen timestamp
            userService.updateLastSeen(user.getId());

            log.info("User login successful: {}", user.getEmail());



            return ResponseEntity.ok(Map.of("accessToken", accessToken, "user", covertUserToUserDto(user)));

        } catch (BadCredentialsException e) {
            log.warn("Failed login attempt for user: {}", request.getEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid credentials"));
        }
    }

    private UserDto covertUserToUserDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .address(user.getAddress())
                .lastSeenAt(user.getLastSeenAt())
                .build();
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(
            @CookieValue(value = REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshTokenFromCookie,
            @RequestParam(required = false) String refreshToken,
            HttpServletResponse response) {

        // Prioritize token from cookie, but accept from request param as fallback
        String tokenValue = refreshTokenFromCookie != null ? refreshTokenFromCookie : refreshToken;

        if (tokenValue == null || tokenValue.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Refresh token is required"));
        }

        try {
            RefreshToken token = refreshTokenService.findByToken(tokenValue);
            refreshTokenService.verifyExpiration(token);

            User user = token.getUser();
            UserDetails userDetails = userService.loadUserByUsername(user.getEmail());

            // Generate new access token
            String newAccessToken = jwtService.generateToken(userDetails);

            // Create new refresh token for rotation
            RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user.getId());

            // Set new refresh token in HTTP-only cookie
            addRefreshTokenCookie(response, newRefreshToken.getToken());

            log.info("Token refreshed for user ID: {}", user.getId());

            return ResponseEntity.ok(Map.of("accessToken", newAccessToken));

        } catch (Exception e) {
            log.warn("Token refresh failed: {}", e.getMessage());
            // Clear the invalid cookie
            deleteRefreshTokenCookie(response);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or expired refresh token"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @CookieValue(value = REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response) {

        // Clear the refresh token cookie
        deleteRefreshTokenCookie(response);

        // If we have a token, invalidate it in the database
        if (refreshToken != null && !refreshToken.isEmpty()) {
            try {
                RefreshToken token = refreshTokenService.findByToken(refreshToken);
                refreshTokenService.deleteByUserId(token.getUser().getId());
                log.info("User logged out, token invalidated for user ID: {}", token.getUser().getId());
            } catch (Exception e) {
                log.warn("Failed to invalidate token during logout: {}", e.getMessage());
                // Continue with logout even if token invalidation fails
            }
        }

        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @DeleteMapping("/tokens/user/{userId}")
    public ResponseEntity<?> revokeAllUserTokens(@PathVariable Long userId) {
        refreshTokenService.deleteByUserId(userId);
        log.info("All refresh tokens revoked for user ID: {}", userId);
        return ResponseEntity.noContent().build();
    }

    // Helper methods
    private void addRefreshTokenCookie(HttpServletResponse response, String token) {
        Cookie refreshTokenCookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, token);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true); // For HTTPS environments
        refreshTokenCookie.setMaxAge((int) (refreshTokenService.getRefreshTokenDurationMs() / 1000));
        refreshTokenCookie.setPath(REFRESH_TOKEN_PATH);
        refreshTokenCookie.setAttribute("SameSite", "Strict"); // Protect against CSRF
        response.addCookie(refreshTokenCookie);

        // Add security headers
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setHeader("Pragma", "no-cache");
    }

    private void deleteRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath(REFRESH_TOKEN_PATH);
        cookie.setMaxAge(0); // Delete cookie
        response.addCookie(cookie);
    }

    private String generateDefaultNickname() {
        return "user_" + UUID.randomUUID().toString().replace("-", "").substring(0, 5);
    }
}