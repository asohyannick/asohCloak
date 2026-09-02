package com.asohCloak.asohCloak.dto.user;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequestDto(
        @NotBlank(message = "Refresh token is required to log out.")
        String refreshToken
) {
}