package com.loopify.mainservice.dto.request.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyCodeRequest(@NotBlank @Email String email, @NotBlank @Size(min = 4, max = 4) String code) {
}
