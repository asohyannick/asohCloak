package com.asohCloak.asohCloak.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload for administratively blocking a user account")
public record BlockAccountRequestDto(

        @Schema(description = "Reason the account is being blocked", example = "Repeated policy violations")
        @NotBlank(message = "A reason is required to block an account")
        @Size(max = 500, message = "Reason must not exceed 500 characters")
        String reason
) { }