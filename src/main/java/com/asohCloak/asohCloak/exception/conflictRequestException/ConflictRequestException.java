package com.asohCloak.asohCloak.exception.conflictRequestException;

import java.io.Serial;

public class ConflictRequestException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String DEFAULT_MESSAGE = "Conflict";
    private static final int DEFAULT_STATUS_CODE = 409;

    private final int statusCode;

    public ConflictRequestException() {
        super(DEFAULT_MESSAGE);
        this.statusCode = DEFAULT_STATUS_CODE;
    }

    public ConflictRequestException(String message) {
        super(message);
        this.statusCode = DEFAULT_STATUS_CODE;
    }

    public ConflictRequestException(Throwable cause) {
        super(DEFAULT_MESSAGE, cause);
        this.statusCode = DEFAULT_STATUS_CODE;
    }

    public ConflictRequestException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = DEFAULT_STATUS_CODE;
    }

    public int getStatusCode() {
        return statusCode;
    }
}