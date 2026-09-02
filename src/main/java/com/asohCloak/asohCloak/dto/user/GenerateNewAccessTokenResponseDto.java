package com.asohCloak.asohCloak.dto.user;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(
        name = "GenerateNewAccessTokenResponse",
        description = "Response containing newly generated access and refresh tokens."
)
public record GenerateNewAccessTokenResponseDto(

        @Schema(
                description = "Newly generated JWT access token.",
                example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
        )
        String accessToken,

        @Schema(
                description = "Newly generated refresh token.",
                example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
        )
        String refreshToken,

        @Schema(
                description = "Timestamp when the token record was created.",
                example = "2026-09-02T16:30:00Z",
                type = "string",
                format = "date-time"
        )
        Instant createdAt,

        @Schema(
                description = "Timestamp when the token record was last updated.",
                example = "2026-09-02T16:30:00Z",
                type = "string",
                format = "date-time"
        )
        Instant updatedAt
) { }