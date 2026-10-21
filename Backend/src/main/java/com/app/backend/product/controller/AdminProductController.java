package com.app.backend.product.controller;

import com.app.backend.common.response.ApiResponse;
import com.app.backend.common.response.PageResponse;
import com.app.backend.product.dto.AdminProductResponse;
import com.app.backend.product.dto.CreateProductRequest;
import com.app.backend.product.dto.PatchProductStatusRequest;
import com.app.backend.product.dto.UpdateProductRequest;
import com.app.backend.product.query.AdminProductQuery;
import com.app.backend.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminProductResponse>>> list(
            @Valid AdminProductQuery query) {
        return ResponseEntity.ok(ApiResponse.success(productService.listAdmin(query),
                "Products retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminProductResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(productService.getAdmin(id),
                "Product retrieved successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AdminProductResponse>> create(
            @Valid @RequestBody CreateProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(productService.create(request), "Product created"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminProductResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request) {
        return ResponseEntity.ok(ApiResponse.success(productService.update(id, request), "Product updated"));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<AdminProductResponse>> patchStatus(
            @PathVariable Long id,
            @Valid @RequestBody PatchProductStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                productService.patchStatus(id, parseStatus(request.status())), "Product status updated"));
    }

    private static com.app.backend.product.ProductStatus parseStatus(String raw) {
        try {
            return com.app.backend.product.ProductStatus.valueOf(raw.trim());
        } catch (IllegalArgumentException e) {
            throw com.app.backend.common.exception.BusinessException.validation(
                    "status must be ACTIVE or INACTIVE",
                    java.util.Map.of("status", "status must be ACTIVE or INACTIVE"));
        }
    }
}
