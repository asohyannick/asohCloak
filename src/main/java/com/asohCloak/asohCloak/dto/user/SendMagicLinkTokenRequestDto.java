package com.asohCloak.asohCloak.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Payload for requesting a passwordless magic-link login email")
public record SendMagicLinkTokenRequestDto(

        @Schema(description = "Email address to send the magic link to", example = "yannick@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        String email
) { }