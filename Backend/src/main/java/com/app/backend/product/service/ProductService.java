package com.app.backend.product.service;

import com.app.backend.category.Category;
import com.app.backend.category.CategoryRepository;
import com.app.backend.common.exception.BusinessException;
import com.app.backend.common.response.PageResponse;
import com.app.backend.product.Product;
import com.app.backend.product.ProductInventory;
import com.app.backend.product.ProductInventoryRepository;
import com.app.backend.product.ProductRepository;
import com.app.backend.product.ProductStatus;
import com.app.backend.product.dto.AdminProductResponse;
import com.app.backend.product.dto.CreateProductRequest;
import com.app.backend.product.dto.ProductResponse;
import com.app.backend.product.dto.UpdateProductRequest;
import com.app.backend.product.query.AdminProductQuery;
import com.app.backend.product.query.ProductSort;
import com.app.backend.product.query.ProductSpecs;
import com.app.backend.product.query.PublicProductQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductInventoryRepository inventoryRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> listPublic(PublicProductQuery query) {
        validatePriceRange(query.minPrice(), query.maxPrice());
        Specification<Product> spec = buildSpec(
                query.search(), query.categoryId(), query.minPrice(), query.maxPrice(),
                ProductSpecs.activeOnly(), false);
        Page<Product> page = productRepository.findAll(spec,
                PageRequest.of(query.page(), query.size(), ProductSort.parse(query.sort())));
        return PageResponse.of(
                page.getContent().stream().map(ProductService::toPublicResponse).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminProductResponse> listAdmin(AdminProductQuery query) {
        validatePriceRange(query.minPrice(), query.maxPrice());
        Specification<Product> spec = buildSpec(
                query.search(), query.categoryId(), query.minPrice(), query.maxPrice(),
                query.status() == null ? null : ProductSpecs.statusIs(query.status()), true);
        Page<Product> page = productRepository.findAll(spec,
                PageRequest.of(query.page(), query.size(), ProductSort.parse(query.sort())));
        List<AdminProductResponse> content = toAdminResponses(page.getContent());
        return PageResponse.of(content,
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Transactional(readOnly = true)
    public ProductResponse getPublic(Long id) {
        Product product = productRepository.findById(id)
                .filter(p -> p.getStatus() == ProductStatus.ACTIVE)
                .orElseThrow(() -> BusinessException.notFound("PRODUCT_NOT_FOUND", "Product not found"));
        return toPublicResponse(product);
    }

    @Transactional(readOnly = true)
    public AdminProductResponse getAdmin(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("PRODUCT_NOT_FOUND", "Product not found"));
        return toAdminResponse(product, stockOf(product.getId()));
    }

    @Transactional
    public AdminProductResponse create(CreateProductRequest request) {
        Category category = findCategory(request.categoryId());
        String sku = request.sku().trim().toUpperCase(Locale.ROOT);
        if (productRepository.existsBySku(sku)) {
            throw BusinessException.conflict("SKU_ALREADY_EXISTS", "SKU already exists");
        }
        Product product = new Product();
        product.setSku(sku);
        product.setName(request.name().trim());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStatus(ProductStatus.ACTIVE);
        product.setCategory(category);
        try {
            productRepository.saveAndFlush(product);
        } catch (DataIntegrityViolationException e) {
            throw BusinessException.conflict("SKU_ALREADY_EXISTS", "SKU already exists");
        }
        ProductInventory inventory = new ProductInventory();
        inventory.setProductId(product.getId());
        inventory.setQuantity(request.initialStock());
        inventoryRepository.save(inventory);
        return toAdminResponse(product, request.initialStock());
    }

    @Transactional
    public AdminProductResponse update(Long id, UpdateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("PRODUCT_NOT_FOUND", "Product not found"));
        if (!product.getVersion().equals(request.version())) {
            throw concurrentModification();
        }
        Category category = findCategory(request.categoryId());
        product.setName(request.name().trim());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStatus(request.status());
        product.setCategory(category);
        try {
            productRepository.saveAndFlush(product);
        } catch (OptimisticLockingFailureException e) {
            throw concurrentModification();
        }
        return toAdminResponse(product, stockOf(product.getId()));
    }

    @Transactional
    public AdminProductResponse patchStatus(Long id, ProductStatus newStatus) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("PRODUCT_NOT_FOUND", "Product not found"));
        if (product.getStatus() != newStatus) {
            product.setStatus(newStatus);
            try {
                productRepository.saveAndFlush(product);
            } catch (OptimisticLockingFailureException e) {
                throw concurrentModification();
            }
        }
        return toAdminResponse(product, stockOf(product.getId()));
    }

    private static Specification<Product> buildSpec(String search, Long categoryId,
                                                    BigDecimal minPrice, BigDecimal maxPrice,
                                                    Specification<Product> extra, boolean adminSearch) {
        List<Specification<Product>> specs = new ArrayList<>();
        if (extra != null) {
            specs.add(extra);
        }
        if (search != null && !search.isBlank()) {
            specs.add(adminSearch
                    ? ProductSpecs.nameOrSkuContains(search.trim())
                    : ProductSpecs.nameContains(search.trim()));
        }
        if (categoryId != null) {
            specs.add(ProductSpecs.categoryIdEquals(categoryId));
        }
        if (minPrice != null || maxPrice != null) {
            specs.add(ProductSpecs.priceBetween(minPrice, maxPrice));
        }
        Specification<Product> result = null;
        for (Specification<Product> s : specs) {
            result = result == null ? s : result.and(s);
        }
        return result;
    }

    private void validatePriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw BusinessException.validation(
                    "minPrice must not be greater than maxPrice",
                    Map.of("minPrice", "minPrice must not be greater than maxPrice"));
        }
    }

    private static BusinessException concurrentModification() {
        return BusinessException.conflict("CONCURRENT_MODIFICATION",
                "Product was modified by another request; reload and retry");
    }

    private Category findCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> BusinessException.notFound("CATEGORY_NOT_FOUND", "Category not found"));
    }

    private long stockOf(Long productId) {
        return inventoryRepository.findById(productId)
                .map(ProductInventory::getQuantity)
                .orElse(0L);
    }

    private List<AdminProductResponse> toAdminResponses(List<Product> products) {
        List<Long> ids = products.stream().map(Product::getId).toList();
        Map<Long, Long> stockById = ids.isEmpty() ? Map.of()
                : inventoryRepository.findAllById(ids).stream()
                        .collect(Collectors.toMap(ProductInventory::getProductId, ProductInventory::getQuantity));
        return products.stream()
                .map(p -> toAdminResponse(p, stockById.getOrDefault(p.getId(), 0L)))
                .toList();
    }

    private static ProductResponse toPublicResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getCreatedAt(),
                product.getUpdatedAt());
    }

    private static AdminProductResponse toAdminResponse(Product product, long stock) {
        return new AdminProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStatus(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                stock,
                product.getVersion(),
                product.getCreatedAt(),
                product.getUpdatedAt());
    }
}
