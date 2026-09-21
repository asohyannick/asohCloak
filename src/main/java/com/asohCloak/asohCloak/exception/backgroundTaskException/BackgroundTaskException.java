package com.asohCloak.asohCloak.exception.backgroundTaskException;

import lombok.Getter;

import java.io.Serial;

@Getter
public class BackgroundTaskException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String DEFAULT_MESSAGE = "Background Task Exception";
    private static final int DEFAULT_STATUS_CODE = 500;

    private final int statusCode;

    public BackgroundTaskException() {
        super(DEFAULT_MESSAGE);
        this.statusCode = DEFAULT_STATUS_CODE;
    }

    public BackgroundTaskException(String message) {
        super(message);
        this.statusCode = DEFAULT_STATUS_CODE;
    }

    public BackgroundTaskException(Throwable cause) {
        super(DEFAULT_MESSAGE, cause);
        this.statusCode = DEFAULT_STATUS_CODE;
    }

    public BackgroundTaskException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = DEFAULT_STATUS_CODE;
    }
}