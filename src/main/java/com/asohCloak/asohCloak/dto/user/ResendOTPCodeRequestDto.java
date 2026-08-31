package com.asohCloak.asohCloak.dto.user;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Payload for requesting a fresh OTP code")
public record ResendOTPCodeRequestDto(

        @Schema(description = "Email address the OTP should be resent to", example = "yannick@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        String email
) { }