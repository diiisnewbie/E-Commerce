package com.app.backend.product.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Nhận status dạng String để kiểm soát lỗi enum → 400 fields.status (TC-PRD-32),
 * thay vì để Jackson ném HttpMessageNotReadableException không có fields.
 */
public record PatchProductStatusRequest(
        @NotBlank(message = "status must not be blank")
        String status) {
}
