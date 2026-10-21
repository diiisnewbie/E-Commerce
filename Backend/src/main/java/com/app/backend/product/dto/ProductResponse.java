package com.app.backend.product.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
        Long id,
        String sku,
        String name,
        String description,
        BigDecimal price,
        Long categoryId,
        String categoryName,
        Instant createdAt,
        Instant updatedAt) {
}
