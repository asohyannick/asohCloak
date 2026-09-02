package com.asohCloak.asohCloak.config.globalSuccessResponse;

public record GlobalSuccessResponse<T>(
        String message,
        T data,
        int statusCode
        ) { }
