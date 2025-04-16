package com.loopify.mainservice.controller.user;

import com.loopify.mainservice.dto.request.user.*;
import com.loopify.mainservice.dto.response.user.AuthResponse;
import com.loopify.mainservice.dto.response.user.MessageResponse;
import com.loopify.mainservice.dto.user.*;
import com.loopify.mainservice.model.user.RefreshToken;
import com.loopify.mainservice.model.user.User;
import com.loopify.mainservice.security.JwtService;
import com.loopify.mainservice.security.RefreshTokenService;
import com.loopify.mainservice.security.CustomUserDetailsService;
import com.loopify.mainservice.service.user.AuthService;
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
    private final AuthService authService;

    @PostMapping("/request-code")
    public ResponseEntity<MessageResponse> requestCode(@Valid @RequestBody RequestCodeRequest request) {
        authService.requestEmailCode(request.email());
        return ResponseEntity.ok(new MessageResponse("Verification code sent successfully."));
    }

    @PostMapping("/verify-code")
    public ResponseEntity<AuthResponse> verifyCode(@Valid @RequestBody VerifyCodeRequest request) {
        AuthResponse authResponse = authService.verifyEmailCode(request.email(), request.code());
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/google/callback")
    public ResponseEntity<AuthResponse> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        AuthResponse authResponse = authService.loginWithGoogle(request.idToken());
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse authResponse = authService.refreshToken(request.refreshToken());
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(@Valid @RequestBody LogoutRequest request) {
        // Assumes frontend sends the refresh token to invalidate
        authService.logout(request.refreshToken());
        return ResponseEntity.ok(new MessageResponse("Logout successful."));
    }

    // --- Exception Handler (Recommended) ---
    @ExceptionHandler(RuntimeException.class) // Catch specific exceptions ideally
    @ResponseStatus(HttpStatus.BAD_REQUEST) // Or appropriate status
    public ResponseEntity<MessageResponse> handleAuthExceptions(RuntimeException ex) {
        // Log the full exception server-side
        return ResponseEntity.badRequest().body(new MessageResponse(ex.getMessage()));
    }
}