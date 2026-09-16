package com.asohCloak.asohCloak.exception.keycloakAuthenticationException;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class KeycloakAuthenticationException extends RuntimeException {

    private final HttpStatus status;

    public KeycloakAuthenticationException(String message) {
        this(message, HttpStatus.UNAUTHORIZED, null);
    }

    public KeycloakAuthenticationException(String message, Throwable cause) {
        this(message, HttpStatus.UNAUTHORIZED, cause);
    }

    public KeycloakAuthenticationException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public static KeycloakAuthenticationException unavailable(Throwable cause) {
        return new KeycloakAuthenticationException(
                "Authentication server is unavailable. Please try again shortly.",
                HttpStatus.SERVICE_UNAVAILABLE, cause);
    }
}