package com.app.backend.product.dto;

import com.app.backend.product.ProductStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record AdminProductResponse(
        Long id,
        String sku,
        String name,
        String description,
        BigDecimal price,
        ProductStatus status,
        Long categoryId,
        String categoryName,
        Long stock,
        Long version,
        Instant createdAt,
        Instant updatedAt) {
}
