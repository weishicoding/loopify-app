package com.loopify.mainservice.controller.user;

import com.loopify.mainservice.dto.request.user.*;
import com.loopify.mainservice.dto.response.user.AuthResponse;
import com.loopify.mainservice.dto.response.user.MessageResponse;
import com.loopify.mainservice.dto.response.user.UserTokenDto;
import com.loopify.mainservice.dto.user.*;
import com.loopify.mainservice.model.user.RefreshToken;
import com.loopify.mainservice.model.user.User;
import com.loopify.mainservice.security.JwtService;
import com.loopify.mainservice.security.RefreshTokenService;
import com.loopify.mainservice.security.CustomUserDetailsService;
import com.loopify.mainservice.service.user.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
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
    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final String REFRESH_TOKEN_PATH = "/api/v1/auth";

    @PostMapping("/request-code")
    public ResponseEntity<MessageResponse> requestCode(@Valid @RequestBody RequestCodeRequest request) {
        authService.requestEmailCode(request.email());
        return ResponseEntity.ok(new MessageResponse("Verification code sent successfully."));
    }

    @PostMapping("/verify-code")
    public ResponseEntity<UserTokenDto> verifyCode(@Valid @RequestBody VerifyCodeRequest request, HttpServletResponse response) {
        AuthResponse authResponse = authService.verifyEmailCode(request.email(), request.code());
        // Set refresh token in HTTP-only cookie
        addRefreshTokenCookie(response, authResponse.refreshToken());
        log.info("User login successful: {}", request.email());
        return ResponseEntity.ok(new UserTokenDto(authResponse.accessToken(), authResponse.user()));
    }

    @PostMapping("/google/callback")
    public ResponseEntity<UserTokenDto> googleLogin(@Valid @RequestBody GoogleLoginRequest request, HttpServletResponse response) {
        AuthResponse authResponse = authService.loginWithGoogle(request.idToken());
        // Set refresh token in HTTP-only cookie
        addRefreshTokenCookie(response, authResponse.refreshToken());
        log.info("User login through google successful");
        return ResponseEntity.ok(new UserTokenDto(authResponse.accessToken(), authResponse.user()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<UserTokenDto> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractRefreshTokenFromCookies(request);
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        AuthResponse authResponse = authService.refreshToken(refreshToken);
        addRefreshTokenCookie(response, refreshToken);
        return ResponseEntity.ok(new UserTokenDto(authResponse.accessToken(), authResponse.user()));
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(@CookieValue(value = REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken, HttpServletResponse response) {
        // Assumes frontend sends the refresh token to invalidate
        authService.logout(refreshToken);
        // Clear the refresh token cookie
        deleteRefreshTokenCookie(response);
        return ResponseEntity.ok(new MessageResponse("Logout successful."));
    }

    // --- Exception Handler (Recommended) ---
    @ExceptionHandler(RuntimeException.class) // Catch specific exceptions ideally
    @ResponseStatus(HttpStatus.BAD_REQUEST) // Or appropriate status
    public ResponseEntity<MessageResponse> handleAuthExceptions(RuntimeException ex) {
        // Log the full exception server-side
        return ResponseEntity.badRequest().body(new MessageResponse(ex.getMessage()));
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

    private String extractRefreshTokenFromCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}