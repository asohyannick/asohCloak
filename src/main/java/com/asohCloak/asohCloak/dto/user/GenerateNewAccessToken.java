package com.asohCloak.asohCloak.dto.user;

import jakarta.validation.constraints.NotBlank;

public record GenerateNewAccessToken(
        @NotBlank(
                message = "Refresh token must be provided for the generation of new access and refresh tokens."
        )
        String refreshToken
) { }