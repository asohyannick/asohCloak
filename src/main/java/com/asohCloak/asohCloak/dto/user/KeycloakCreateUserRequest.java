package com.asohCloak.asohCloak.dto.user;
import com.asohCloak.asohCloak.exception.badRequestException.BadRequestException;

import java.util.regex.Pattern;

public record KeycloakCreateUserRequest(
        String email,
        String firstName,
        String lastName,
        String password,
        String roleName,
        boolean emailVerified
) {
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public KeycloakCreateUserRequest {
        if (email == null || !EMAIL.matcher(email.trim()).matches()) {
            throw new BadRequestException("Invalid email passed to Keycloak user creation: " + email);
        }
        if (password == null || password.isBlank()) {
            throw new BadRequestException("Password is required for Keycloak user creation.");
        }
        if (roleName == null || roleName.isBlank()) {
            throw new BadRequestException("Role is required for Keycloak user creation.");
        }
        email = email.trim().toLowerCase();
        firstName = firstName == null ? "" : firstName.trim();
        lastName = lastName == null ? "" : lastName.trim();
    }
}