package com.asohCloak.asohCloak.dto.user;

import jakarta.validation.constraints.NotBlank;

public record VerifyFirebaseIDTokenRequestDto(
        @NotBlank(message = "Firebase ID token must be provided")
        String idToken
) { }
