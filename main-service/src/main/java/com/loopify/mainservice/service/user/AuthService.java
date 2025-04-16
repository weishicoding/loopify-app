package com.loopify.mainservice.service.user;

import com.loopify.mainservice.dto.response.user.AuthResponse;

public interface AuthService {

    void requestEmailCode(String email);

    AuthResponse verifyEmailCode(String email, String code);

    AuthResponse loginWithGoogle(String idTokenString);

    AuthResponse refreshToken(String requestRefreshToken);

    void logout(String refreshToken);
}
