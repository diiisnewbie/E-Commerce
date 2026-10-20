package com.app.backend.auth.exception;

import org.springframework.http.HttpStatus;

/**
 * Business exception của auth — GlobalExceptionHandler map sang ApiResponse với status/mã lỗi tương ứng (6.4).
 */
public class AuthException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public AuthException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public static AuthException invalidCredentials() {
        return new AuthException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password");
    }

    public static AuthException emailExists() {
        return new AuthException(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "Email already exists");
    }

    public static AuthException token(String errorCode, String message) {
        return new AuthException(HttpStatus.UNAUTHORIZED, errorCode, message);
    }

    public static AuthException validation(String message) {
        return new AuthException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }
}
