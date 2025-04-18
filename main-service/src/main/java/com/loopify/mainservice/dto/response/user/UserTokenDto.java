package com.loopify.mainservice.dto.response.user;

import com.loopify.mainservice.dto.user.UserDto;

public record UserTokenDto(String accessToken, UserDto user) {
}
