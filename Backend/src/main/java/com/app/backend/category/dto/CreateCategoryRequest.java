package com.app.backend.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
        @NotBlank(message = "name must not be blank")
        @Size(min = 2, max = 100, message = "name must be between 2 and 100 characters")
        String name,

        @Size(max = 500, message = "description must be at most 500 characters")
        String description) {
}
