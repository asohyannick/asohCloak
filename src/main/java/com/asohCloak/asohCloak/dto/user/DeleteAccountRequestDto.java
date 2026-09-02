package com.asohCloak.asohCloak.dto.user;

import jakarta.validation.constraints.NotBlank;

public record DeleteAccountRequestDto(
        @NotBlank(message = "Password is required to confirm account deletion.")
        String password,

        String reason
) {
}