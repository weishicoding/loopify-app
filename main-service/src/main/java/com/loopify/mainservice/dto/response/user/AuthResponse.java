package com.loopify.mainservice.dto.response.user;

import com.loopify.mainservice.dto.user.UserDto;

public record AuthResponse(String accessToken, String refreshToken, UserDto user) {
}
