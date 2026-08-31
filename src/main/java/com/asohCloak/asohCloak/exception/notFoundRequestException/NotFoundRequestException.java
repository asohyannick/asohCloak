package com.asohCloak.asohCloak.exception.notFoundRequestException;

import java.io.Serial;

public class NotFoundRequestException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String DEFAULT_MESSAGE = "Not Found";
    private static final int DEFAULT_STATUS_CODE = 404;

    private final int statusCode;

    public NotFoundRequestException() {
        super(DEFAULT_MESSAGE);
        this.statusCode = DEFAULT_STATUS_CODE;
    }

    public NotFoundRequestException(String message) {
        super(message);
        this.statusCode = DEFAULT_STATUS_CODE;
    }

    public NotFoundRequestException(Throwable cause) {
        super(DEFAULT_MESSAGE, cause);
        this.statusCode = DEFAULT_STATUS_CODE;
    }

    public NotFoundRequestException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = DEFAULT_STATUS_CODE;
    }

    public int getStatusCode() {
        return statusCode;
    }
}