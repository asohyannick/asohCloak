package com.asohCloak.asohCloak.dto.user;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Payload for verifying a one-time passcode")
public record VerifyOTPCodeRequestDto(

        @Schema(description = "6-digit one-time passcode", example = "482913")
        @NotBlank(message = "OTP code is required")
        @Pattern(regexp = "^\\d{6}$", message = "OTP code must be exactly 6 digits")
        String otpCode
) { }