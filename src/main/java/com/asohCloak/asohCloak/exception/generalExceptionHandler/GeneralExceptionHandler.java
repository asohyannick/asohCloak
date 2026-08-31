package com.asohCloak.asohCloak.exception.generalExceptionHandler;

import com.asohCloak.asohCloak.exception.conflictRequestException.ConflictRequestException;
import com.asohCloak.asohCloak.exception.forbiddenRequestException.ForbiddenRequestException;
import com.asohCloak.asohCloak.exception.globalExceptionResponseHandler.GlobalExceptionResponseHandler;
import com.asohCloak.asohCloak.exception.internalServerErrorRequestException.InternalServerErrorRequestException;
import com.asohCloak.asohCloak.exception.notFoundRequestException.NotFoundRequestException;
import com.asohCloak.asohCloak.exception.unauthorizedRequestException.UnAuthorizedRequestException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import java.time.Instant;
@RestControllerAdvice
public class GeneralExceptionHandler {

    private ResponseEntity<GlobalExceptionResponseHandler> buildResponse(
            String message,
            String details,
            String statusCode,
            String errorCode,
            HttpStatus status,
            HttpServletRequest request
    ) {

        GlobalExceptionResponseHandler response =
                new GlobalExceptionResponseHandler(
                        Instant.now(),
                        message,
                        details,
                        statusCode,
                        status.value(),
                        errorCode,
                        request.getRequestURI(),
                        request.getMethod()
                );

        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler( NotFoundRequestException.class)
    public ResponseEntity<GlobalExceptionResponseHandler> handleNotFoundException(
            NoHandlerFoundException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                ex.getMessage(),
                "Resource not found!",
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                "NOT_FOUND",
                HttpStatus.NOT_FOUND,
                request
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GlobalExceptionResponseHandler> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");

        return buildResponse(
                errorMessage,
                "Invalid request payload",
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "BAD_REQUEST",
                HttpStatus.BAD_REQUEST,
                request
        );
    }

    @ExceptionHandler( UnAuthorizedRequestException.class)
    public ResponseEntity<GlobalExceptionResponseHandler> handleUnauthorizedException(
            UnAuthorizedRequestException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                ex.getMessage(),
                "Authentication failed",
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                "UNAUTHORIZED",
                HttpStatus.UNAUTHORIZED,
                request
        );
    }

    @ExceptionHandler( ForbiddenRequestException.class)
    public ResponseEntity<GlobalExceptionResponseHandler> handleAccessDeniedException(
            ForbiddenRequestException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                ex.getMessage(),
                "Access denied",
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                "FORBIDDEN",
                HttpStatus.FORBIDDEN,
                request
        );
    }

    @ExceptionHandler( ConflictRequestException.class)
    public ResponseEntity<GlobalExceptionResponseHandler> handleConflictException(
            ConflictRequestException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                ex.getMessage(),
                "Conflict occurred",
                HttpStatus.CONFLICT.getReasonPhrase(),
                "CONFLICT",
                HttpStatus.CONFLICT,
                request
        );
    }

    @ExceptionHandler( InternalServerErrorRequestException.class)
    public ResponseEntity<GlobalExceptionResponseHandler> handleGlobalException(
            InternalServerErrorRequestException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                ex.getMessage(),
                "An unexpected error occurred",
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "INTERNAL_SERVER_ERROR",
                HttpStatus.INTERNAL_SERVER_ERROR,
                request
        );
    }
}