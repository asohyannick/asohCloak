package com.asohCloak.asohCloak.dto.user;

import com.asohCloak.asohCloak.enums.UserRole;

import java.time.Instant;
import java.util.UUID;

public record UserResponseDto(
    UUID id,
    String firstName,
    String lastName,
    String email,
    UserRole role,
    boolean accountVerified,
    boolean accountLocked,
    boolean accountSuspended,
    Instant createdAt,
    Instant updatedAt
) {}
