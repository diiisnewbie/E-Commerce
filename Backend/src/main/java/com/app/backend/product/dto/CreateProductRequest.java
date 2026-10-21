package com.app.backend.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank(message = "sku must not be blank")
        @Size(min = 3, max = 50, message = "sku must be between 3 and 50 characters")
        @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "sku must contain only letters, digits, underscore or hyphen")
        String sku,

        @NotBlank(message = "name must not be blank")
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

        @NotNull(message = "initialStock must not be null")
        @Min(value = 0, message = "initialStock must be >= 0")
        Long initialStock) {
}
