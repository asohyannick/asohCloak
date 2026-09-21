package com.asohCloak.asohCloak.exception.storageException;

import lombok.Getter;

import java.io.Serial;

@Getter
public class StorageException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String DEFAULT_MESSAGE = "Storage Exception";
    private static final int DEFAULT_STATUS_CODE = 500;

    private final int statusCode;

    public StorageException() {
        super(DEFAULT_MESSAGE);
        this.statusCode = DEFAULT_STATUS_CODE;
    }

    public StorageException(String message) {
        super(message);
        this.statusCode = DEFAULT_STATUS_CODE;
    }

    public StorageException(Throwable cause) {
        super(DEFAULT_MESSAGE, cause);
        this.statusCode = DEFAULT_STATUS_CODE;
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = DEFAULT_STATUS_CODE;
    }
}