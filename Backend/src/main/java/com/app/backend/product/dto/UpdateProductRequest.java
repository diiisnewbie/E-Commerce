package com.app.backend.product.dto;

import com.app.backend.product.ProductStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateProductRequest(
        @NotNull(message = "name must not be null")
        @Size(min = 2, max = 200, message = "name must be between 2 and 200 characters")
        String name,

        @Size(max = 5000, message = "description must be at most 5000 characters")
        String description,

        @NotNull(message = "price must not be null")
        @DecimalMin(value = "0", inclusive = false, message = "price must be greater than 0")
        @Digits(integer = 17, fraction = 2, message = "price must have at most 17 integer digits and 2 decimals")
        BigDecimal price,

        @NotNull(message = "categoryId must not be null")
        Long categoryId,

        @NotNull(message = "status must not be null")
        ProductStatus status,

        @NotNull(message = "version must not be null")
        Long version) {
}
