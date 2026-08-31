package com.asohCloak.asohCloak.dto.user;

import com.asohCloak.asohCloak.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Response returned after a successful login")
public record LoginResponseDto(

        @Schema(description = "Internal user ID") UUID id,
        @Schema(description = "User's first name") String firstName,
        @Schema(description = "User's last name") String lastName,
        @Schema(description = "User's email address") String email,
        @Schema(description = "Assigned role") UserRole role,
        @Schema(description = "Short-lived JWT access token issued by Keycloak") String accessToken,
        @Schema(description = "Refresh token issued by Keycloak, used to obtain a new access token") String refreshToken
) { }