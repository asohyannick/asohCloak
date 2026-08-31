package com.asohCloak.asohCloak.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Payload for initiating a password-reset flow")
public record ForgotPasswordRequestDto(

        @Schema(description = "Email address of the account requesting a password reset", example = "yannick@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        String email
) { }