package com.asohCloak.asohCloak.exception.badRequestException;

import java.io.Serial;

public class BadRequestException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String DEFAULT_MESSAGE = "Bad Request Exception";
    private static final int DEFAULT_STATUS_CODE = 400;

    private final int statusCode;

    public BadRequestException() {
        super(DEFAULT_MESSAGE);
        this.statusCode = DEFAULT_STATUS_CODE;
    }

    public BadRequestException(String message) {
        super(message);
        this.statusCode = DEFAULT_STATUS_CODE;
    }

    public BadRequestException(Throwable cause) {
        super(DEFAULT_MESSAGE, cause);
        this.statusCode = DEFAULT_STATUS_CODE;
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = DEFAULT_STATUS_CODE;
    }

    public int getStatusCode() {
        return statusCode;
    }
}