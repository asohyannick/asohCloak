package com.asohCloak.asohCloak.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeleteAccountRequestDto(
        @NotBlank(message = "Your password is required to confirm account deletion.")
        String password,

        @Size(max = 500, message = "Reason must not exceed 500 characters.")
        String reason
) {
        public String reasonOrDefault() {
                return (reason == null || reason.isBlank()) ? "No reason provided." : reason.trim();
        }
}