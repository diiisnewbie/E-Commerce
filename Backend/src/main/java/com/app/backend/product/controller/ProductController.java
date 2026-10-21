package com.app.backend.product.controller;

import com.app.backend.common.response.ApiResponse;
import com.app.backend.common.response.PageResponse;
import com.app.backend.product.dto.ProductResponse;
import com.app.backend.product.query.PublicProductQuery;
import com.app.backend.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> listPublic(
            @Valid PublicProductQuery query) {
        return ResponseEntity.ok(ApiResponse.success(productService.listPublic(query),
                "Products retrieved successfully"));
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getPublic(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(productService.getPublic(id),
                "Product retrieved successfully"));
    }
}
