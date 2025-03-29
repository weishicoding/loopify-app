package com.loopify.mainservice.controller.user;

import com.loopify.mainservice.dto.user.LoginRequest;
import com.loopify.mainservice.dto.user.RegisterRequest;
import com.loopify.mainservice.dto.user.UserDto;
import com.loopify.mainservice.model.user.RefreshToken;
import com.loopify.mainservice.model.user.User;
import com.loopify.mainservice.security.JwtService;
import com.loopify.mainservice.security.RefreshTokenService;
import com.loopify.mainservice.security.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        // Check if email already exists
        if (userService.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email already in use"));
        }

        // Create new user
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .isActive(true)
                .build();

        User savedUser = userService.save(user);
        log.info("User registered: {}", savedUser.getEmail());

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "User registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        // Authenticate the user
        try {
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
            Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken.getToken());
            refreshTokenCookie.setHttpOnly(true);
            refreshTokenCookie.setSecure(true); // For HTTPS environments
            refreshTokenCookie.setMaxAge((int) (refreshTokenService.getRefreshTokenDurationMs() / 1000));
            refreshTokenCookie.setPath("/api/v1/auth"); // Restrict to auth endpoints
            response.addCookie(refreshTokenCookie);

            // Update last seen timestamp
            userService.updateLastSeen(user.getId());

            log.info("User logged in: {}", user.getEmail());

            return ResponseEntity.ok(Map.of(
                    "accessToken", accessToken,
                    "user", UserDto.fromUser(user)
            ));

        } catch (BadCredentialsException e) {
            log.warn("Login failed for user: {}", request.getEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials"));
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(
            @CookieValue(value = "refreshToken", required = false) String refreshTokenFromCookie,
            @RequestParam(required = false) String refreshToken,
            HttpServletResponse response) {

        // Prioritize token from cookie, but accept from request param as fallback
        String tokenValue = refreshTokenFromCookie != null ? refreshTokenFromCookie : refreshToken;

        if (tokenValue == null || tokenValue.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Refresh token is required"));
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
            Cookie refreshTokenCookie = new Cookie("refreshToken", newRefreshToken.getToken());
            refreshTokenCookie.setHttpOnly(true);
            refreshTokenCookie.setSecure(true);
            refreshTokenCookie.setMaxAge((int) (refreshTokenService.getRefreshTokenDurationMs() / 1000));
            refreshTokenCookie.setPath("/api/v1/auth");
            response.addCookie(refreshTokenCookie);

            log.info("Token refreshed for user: {}", user.getEmail());

            return ResponseEntity.ok(Map.of(
                    "accessToken", newAccessToken
            ));

        } catch (Exception e) {
            log.warn("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/refresh-token/{userId}")
    public ResponseEntity<?> deleteRefreshToken(@PathVariable Long userId) {
        refreshTokenService.deleteByUserId(userId);
        log.info("Refresh tokens deleted for user ID: {}", userId);
        return ResponseEntity.noContent().build();
    }
}
