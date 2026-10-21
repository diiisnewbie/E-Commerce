package com.app.backend.common.exception;

import com.app.backend.auth.exception.AuthException;
import com.app.backend.common.response.ApiResponse;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuth(AuthException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(ApiResponse.error(ex.getMessage(), ex.getErrorCode()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleBusiness(BusinessException ex) {
        if (ex.getFields() == null || ex.getFields().isEmpty()) {
            return ResponseEntity.status(ex.getStatus())
                    .body(ApiResponse.error(ex.getMessage(), ex.getErrorCode()));
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("fields", ex.getFields());
        return ResponseEntity.status(ex.getStatus())
                .body(new ApiResponse<>(false, data, ex.getMessage(), ex.getErrorCode(), Instant.now()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(validationBody(fields));
    }

    // Phase 2 — query/param lỗi không được thành 500 (4.2, 6.4)
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleMethodValidation(HandlerMethodValidationException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getParameterValidationResults().forEach(result -> {
            String param = result.getMethodParameter().getParameterName();
            result.getResolvableErrors()
                    .forEach(err -> fields.putIfAbsent(param, err.getDefaultMessage()));
        });
        return ResponseEntity.badRequest().body(validationBody(fields));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        String message = "Invalid value for parameter '" + ex.getName() + "'";
        if (ex.getRequiredType() != null) {
            message += " (expected " + ex.getRequiredType().getSimpleName() + ")";
        }
        fields.put(ex.getName(), message);
        return ResponseEntity.badRequest().body(validationBody(fields));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleBind(BindException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getFieldErrors()
                .forEach(error -> fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(validationBody(fields));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("Malformed request body", "VALIDATION_ERROR"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error("Method " + ex.getMethod() + " not allowed", "METHOD_NOT_ALLOWED"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Resource not found", "NOT_FOUND"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Access denied", "ACCESS_DENIED"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        Optional<String> constraint = constraintName(ex);
        String name = constraint.orElseGet(() -> {
            String cause = ex.getMostSpecificCause().getMessage();
            return cause == null ? "" : cause;
        });
        if (name.contains("uk_users_email")) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("Email already exists", "EMAIL_ALREADY_EXISTS"));
        }
        if (name.contains("uk_products_sku")) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("SKU already exists", "SKU_ALREADY_EXISTS"));
        }
        if (name.contains("uk_categories_name")) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("Category name already exists", "CATEGORY_NAME_ALREADY_EXISTS"));
        }
        if (name.contains("fk_products_category")) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("Category still has products or was deleted concurrently",
                            "CATEGORY_HAS_PRODUCTS"));
        }
        if (name.contains("ck_products_price_positive")
                || name.contains("ck_inventory_quantity_non_negative")) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Validation failed", "VALIDATION_ERROR"));
        }
        log.error("Data integrity violation", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred", "INTERNAL_SERVER_ERROR"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred", "INTERNAL_SERVER_ERROR"));
    }

    private static ApiResponse<Map<String, Object>> validationBody(Map<String, String> fields) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("fields", fields);
        return new ApiResponse<>(false, data, "Validation failed", "VALIDATION_ERROR", Instant.now());
    }

    /** Đọc tên constraint có cấu trúc từ Hibernate exception (6.3) — không soi chuỗi message thô làm bước đầu. */
    private static Optional<String> constraintName(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof ConstraintViolationException c && c.getConstraintName() != null) {
                return Optional.of(c.getConstraintName());
            }
        }
        return Optional.empty();
    }
}
