package com.asohCloak.asohCloak.exception.forbiddenRequestException;

import java.io.Serial;

public class ForbiddenRequestException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String DEFAULT_MESSAGE = "Forbidden";
    private static final int DEFAULT_STATUS_CODE = 403;

    private final int statusCode;

    public ForbiddenRequestException() {
        super(DEFAULT_MESSAGE);
        this.statusCode = DEFAULT_STATUS_CODE;
    }

    public ForbiddenRequestException(String message) {
        super(message);
        this.statusCode = DEFAULT_STATUS_CODE;
    }

    public ForbiddenRequestException(Throwable cause) {
        super(DEFAULT_MESSAGE, cause);
        this.statusCode = DEFAULT_STATUS_CODE;
    }

    public ForbiddenRequestException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = DEFAULT_STATUS_CODE;
    }

    public int getStatusCode() {
        return statusCode;
    }
}