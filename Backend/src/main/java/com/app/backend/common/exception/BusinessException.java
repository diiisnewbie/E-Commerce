package com.app.backend.common.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Business exception dùng chung cho Phase 2+ (D-2-08).
 * Class (không phải record) vì phải kế thừa RuntimeException để throw được.
 * AuthException của Phase 1 giữ nguyên — không sửa logic phase đã Done (AGENTS mục 2).
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;
    private final Map<String, String> fields;

    public BusinessException(HttpStatus status, String errorCode, String message) {
        this(status, errorCode, message, Map.of());
    }

    public BusinessException(HttpStatus status, String errorCode, String message, Map<String, String> fields) {
        super(message, null, false, false); // lỗi nghiệp vụ dự kiến: bỏ chi phí capture stack trace
        this.status = status;
        this.errorCode = errorCode;
        this.fields = fields;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Map<String, String> getFields() {
        return fields;
    }

    public static BusinessException notFound(String errorCode, String message) {
        return new BusinessException(HttpStatus.NOT_FOUND, errorCode, message);
    }

    public static BusinessException conflict(String errorCode, String message) {
        return new BusinessException(HttpStatus.CONFLICT, errorCode, message);
    }

    public static BusinessException validation(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    public static BusinessException validation(String message, Map<String, String> fields) {
        return new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, fields);
    }
}
