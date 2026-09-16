package com.asohCloak.asohCloak.dto.user;
import com.asohCloak.asohCloak.entity.user.User;
import com.asohCloak.asohCloak.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Response returned after a successful login")
public record LoginResponseDto(

        @Schema(description = "Internal user ID") UUID id,
        @Schema(description = "User's first name") String firstName,
        @Schema(description = "User's last name") String lastName,
        @Schema(description = "User's email address") String email,
        @Schema(description = "Assigned role") UserRole role,
        @Schema(description = "User's account verified") boolean accountVerified,

        @Schema(description = "Short-lived JWT access token issued by Keycloak") String accessToken,
        @Schema(description = "Lifetime of the access token in seconds", example = "300") Long accessTokenExpiresIn,
        @Schema(description = "UTC time when the access token expires", example = "2026-09-16T13:05:35Z") Instant accessTokenExpiresAt,

        @Schema(description = "Refresh token issued by Keycloak, used to obtain a new access token") String refreshToken,
        @Schema(description = "Lifetime of the refresh token in seconds (null if it has no fixed expiry)", example = "1800") Long refreshTokenExpiresIn,
        @Schema(description = "UTC time when the refresh token expires (null if it has no fixed expiry)", example = "2026-09-16T13:30:35Z") Instant refreshTokenExpiresAt,

        @Schema(description = "Token type to use in the Authorization header", example = "Bearer") String tokenType
) {

    public static LoginResponseDto of(User user, KeycloakTokenResponse tokens) {
        Instant now = Instant.now();

        Long accessTtl = tokens.expiresIn();
        Long refreshTtl = (tokens.refreshExpiresIn() == null || tokens.refreshExpiresIn() == 0)
                ? null
                : tokens.refreshExpiresIn();

        return new LoginResponseDto(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                user.isAccountVerified(),
                tokens.accessToken(),
                accessTtl,
                accessTtl == null ? null : now.plusSeconds(accessTtl),
                tokens.refreshToken(),
                refreshTtl,
                refreshTtl == null ? null : now.plusSeconds(refreshTtl),
                tokens.tokenType() == null ? "Bearer" : tokens.tokenType()
        );
    }
}