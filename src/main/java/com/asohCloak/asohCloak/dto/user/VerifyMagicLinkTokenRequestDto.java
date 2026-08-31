package com.asohCloak.asohCloak.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Payload for completing login via a magic-link token")
public record VerifyMagicLinkTokenRequestDto(

        @Schema(description = "Magic-link token received by email", example = "eyJhbGciOiJIUzI1NiIs...")
        @NotBlank(message = "Magic link token is required")
        String magicLinkToken
) { }