package com.asohCloak.asohCloak.dto.user;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Payload for logging in with email and password")
public record LoginRequestDto(

        @Schema(description = "Registered email address", example = "yannick@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        String email,

        @Schema(description = "Account password", example = "Str0ng!Pass")
        @NotBlank(message = "Password is required")
        String password
) { }