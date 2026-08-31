package com.asohCloak.asohCloak.exception.unauthorizedRequestException;

import java.io.Serial;

public class UnAuthorizedRequestException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String DEFAULT_MESSAGE = "Unauthorized";
    private static final int DEFAULT_STATUS_CODE = 401;

    private final int statusCode;

    public UnAuthorizedRequestException() {
        super(DEFAULT_MESSAGE);
        this.statusCode = DEFAULT_STATUS_CODE;
    }

    public UnAuthorizedRequestException(String message) {
        super(message);
        this.statusCode = DEFAULT_STATUS_CODE;
    }

    public UnAuthorizedRequestException(Throwable cause) {
        super(DEFAULT_MESSAGE, cause);
        this.statusCode = DEFAULT_STATUS_CODE;
    }

    public UnAuthorizedRequestException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = DEFAULT_STATUS_CODE;
    }

    public int getStatusCode() {
        return statusCode;
    }
}