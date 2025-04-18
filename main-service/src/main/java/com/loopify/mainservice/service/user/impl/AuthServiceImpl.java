package com.loopify.mainservice.service.user.impl;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.loopify.mainservice.dto.response.user.AuthResponse;
import com.loopify.mainservice.dto.user.UserDto;
import com.loopify.mainservice.model.user.RefreshToken;
import com.loopify.mainservice.model.user.User;
import com.loopify.mainservice.repository.user.UserRepository;
import com.loopify.mainservice.security.JwtService;
import com.loopify.mainservice.security.RefreshTokenService;
import com.loopify.mainservice.service.EmailService;
import com.loopify.mainservice.service.user.AuthService;
import com.loopify.mainservice.utils.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final StringRedisTemplate redisTemplate;

    @Value("${app.email-code.ttl-minutes}")
    private long codeTtlMinutes;
    @Value("${app.email-code.length}")
    private int codeLength;
    @Value("${app.google.client-id}")
    private String googleClientId;

    private static final String EMAIL_CODE_PREFIX = "email_code:";

    @Override
    @Transactional
    public void requestEmailCode(String email) {
        // Basic validation or rely on controller validation
        String code = Utils.generateNumericCode(codeLength);
        String redisKey = EMAIL_CODE_PREFIX + email;
        redisTemplate.opsForValue().set(redisKey, code, Duration.ofMinutes(codeTtlMinutes));
        log.debug("Stored verification code for email: {}", email); // Don't log the code itself ideally
        emailService.sendVerificationCode(email, code);
    }

    @Override
    @Transactional
    public AuthResponse verifyEmailCode(String email, String code) {
        String redisKey = EMAIL_CODE_PREFIX + email;
        String storedCode = redisTemplate.opsForValue().get(redisKey);

        if (storedCode == null || !storedCode.equals(code)) {
            log.warn("Invalid or expired code attempt for email: {}", email);
            throw new RuntimeException("Invalid or expired verification code."); // Or specific exception
        }

        redisTemplate.delete(redisKey); // Code is valid, consume it

        Optional<User> existingUserOpt = userRepository.findByEmail(email);
        User user;

        if (existingUserOpt.isPresent()) {
            user = existingUserOpt.get();
            if (!user.getIsActive()) {
                log.warn("Login attempt for inactive user: {}", email);
                throw new RuntimeException("Account is disabled.");
            }
            log.info("Existing user logged in via email code: {}", email);
        } else {
            user = User.builder()
                    .email(email)
                    .nickname(generateDefaultNickname())
                    .password(null) // No password for email code flow
                    .authProvider(User.AuthProvider.EMAIL_CODE)
                    .isActive(true)
                    .build();
            user = userRepository.save(user);
            log.info("New user registered via email code: {}", email);
        }

        return generateAndSaveTokens(user);
    }

    @Override
    @Transactional
    public AuthResponse loginWithGoogle(String idTokenString) {
        GoogleIdToken.Payload payload = verifyGoogleIdToken(idTokenString);
        if (payload == null) {
            throw new RuntimeException("Invalid Google ID token.");
        }

        String googleId = payload.getSubject();
        String email = payload.getEmail();
        boolean emailVerified = payload.getEmailVerified();
        String name = (String) payload.get("name");
        String pictureUrl = (String) payload.get("picture");

        if (!emailVerified) {
            throw new RuntimeException("Google email not verified.");
        }

        // Prioritize finding by googleId if available, then by email
        Optional<User> userOpt = userRepository.findByGoogleId(googleId)
                .or(() -> userRepository.findByEmail(email));

        User user;
        if (userOpt.isPresent()) {
            user = userOpt.get();
            if (!user.getIsActive()) {
                log.warn("Login attempt for inactive Google user: {}", email);
                throw new RuntimeException("Account is disabled.");
            }
            // Update user details if needed (e.g., sync googleId, name, avatar)
            boolean updated = false;
            if (user.getGoogleId() == null) {
                user.setGoogleId(googleId);
                updated = true;
            }
            if (pictureUrl != null && !pictureUrl.equals(user.getAvatarUrl())) {
                user.setAvatarUrl(pictureUrl);
                updated = true;
            }
            if (updated) {
                user = userRepository.save(user);
            }
            log.info("Existing user logged in via Google: {}", email);

        } else {
            // Check if nickname derived from Google name is taken
            String potentialNickname = name != null ? name : email.split("@")[0]; // Simple default
            if (userRepository.existsByNickname(potentialNickname)) {
                // Handle nickname conflict - maybe append random numbers or require user input
                potentialNickname = potentialNickname + "_" + Utils.generateNumericCode(4);
                log.warn("Generated conflicting nickname for Google user {}, using {}", email, potentialNickname);
            }

            user = User.builder()
                    .email(email)
                    .nickname(potentialNickname)
                    .avatarUrl(pictureUrl)
                    .password(null)
                    .googleId(googleId)
                    .authProvider(User.AuthProvider.GOOGLE)
                    .isActive(true)
                    .build();
            user = userRepository.save(user);
            log.info("New user registered via Google: {}", email);
        }

        return generateAndSaveTokens(user);
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(String requestRefreshToken) {
        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    // Generate new Access token
                    String newAccessToken = jwtService.generateToken(user);
                    return new AuthResponse(newAccessToken, requestRefreshToken, covertUserToUserDto(user));
                })
                .orElseThrow(() -> new RuntimeException("Refresh token is not in database!"));
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        // Find and delete the specific refresh token
        Optional<RefreshToken> tokenOpt = refreshTokenService.findByToken(refreshToken);
        if(tokenOpt.isPresent()){
            refreshTokenService.revokeAllUserTokens(tokenOpt.get().getUser());
            log.info("User logged out, refresh token invalidated: {}", refreshToken.substring(0, 6) + "...");
        } else {
            log.warn("Logout attempt with non-existent refresh token: {}", refreshToken.substring(0, 6) + "...");
            // Decide if this should be an error or just logged
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

    private AuthResponse generateAndSaveTokens(User user) {
        String accessToken = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());
        return new AuthResponse(accessToken, refreshToken.getToken(), covertUserToUserDto(user));
    }


    private GoogleIdToken.Payload verifyGoogleIdToken(String idTokenString) {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(googleClientId))
                .build();
        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken != null) {
                return idToken.getPayload();
            } else {
                log.error("Google ID Token verification failed (token invalid or expired).");
                return null;
            }
        } catch (GeneralSecurityException | IOException | IllegalArgumentException e) {
            log.error("Error verifying Google ID Token: {}", e.getMessage());
            return null;
        }
    }

    private String generateDefaultNickname() {
        return "user_" + UUID.randomUUID().toString().replace("-", "").substring(0, 5);
    }
}
