package com.asohCloak.asohCloak.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload for administratively unblocking a user account")
public record UnblockAccountRequestDto(

        @Schema(description = "Optional reason the account is being unblocked", example = "Appeal reviewed and approved")
        @Size(max = 500, message = "Reason must not exceed 500 characters")
        String reason
) { }