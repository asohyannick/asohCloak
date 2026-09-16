package com.asohCloak.asohCloak.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "New token pair issued from a refresh token")
public record GenerateNewAccessTokenResponseDto(
        @Schema(description = "New short-lived access token") String accessToken,
        @Schema(description = "Access token lifetime in seconds", example = "300") Long accessTokenExpiresIn,
        @Schema(description = "UTC time when the access token expires") Instant accessTokenExpiresAt,
        @Schema(description = "New refresh token; the previous one is now invalid") String refreshToken,
        @Schema(description = "Refresh token lifetime in seconds", example = "1800") Long refreshTokenExpiresIn,
        @Schema(description = "UTC time when the refresh token expires") Instant refreshTokenExpiresAt,
        @Schema(description = "Token type", example = "Bearer") String tokenType
) { }