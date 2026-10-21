package com.app.backend.product.query;

import com.app.backend.product.ProductStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Query params cho admin list (D-2-12: thêm status optional).
 * Compact constructor áp mặc định trước khi validation chạy (record binding).
 */
public record AdminProductQuery(
        @Min(value = 0, message = "page must be >= 0") Integer page,
        @Min(value = 1, message = "size must be >= 1") @Max(value = 50, message = "size must be <= 50") Integer size,
        String sort,
        @Size(max = 100, message = "search must be at most 100 characters") String search,
        Long categoryId,
        @DecimalMin(value = "0", message = "minPrice must be >= 0") BigDecimal minPrice,
        @DecimalMin(value = "0", message = "maxPrice must be >= 0") BigDecimal maxPrice,
        ProductStatus status) {

    public AdminProductQuery {
        if (page == null) {
            page = 0;
        }
        if (size == null) {
            size = 20;
        }
    }
}
