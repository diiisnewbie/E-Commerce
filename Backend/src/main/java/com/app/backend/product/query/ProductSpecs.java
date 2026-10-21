package com.app.backend.product.query;

import com.app.backend.product.Product;
import com.app.backend.product.ProductStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Specifications cho product list (D-2-06): escape wildcard, lower() cho search.
 */
public final class ProductSpecs {

    private ProductSpecs() {
    }

    public static Specification<Product> activeOnly() {
        return (root, query, cb) -> cb.equal(root.get("status"), ProductStatus.ACTIVE);
    }

    public static Specification<Product> statusIs(ProductStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Product> categoryIdEquals(Long categoryId) {
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Product> priceBetween(BigDecimal minPrice, BigDecimal maxPrice) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Product> nameContains(String search) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + escapeLike(search.toLowerCase(Locale.ROOT)) + "%", '\\');
    }

    public static Specification<Product> nameOrSkuContains(String search) {
        return (root, query, cb) -> {
            String pattern = "%" + escapeLike(search.toLowerCase(Locale.ROOT)) + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("name")), pattern, '\\'),
                    cb.like(cb.lower(root.get("sku")), pattern, '\\'));
        };
    }

    static String escapeLike(String input) {
        return input
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
