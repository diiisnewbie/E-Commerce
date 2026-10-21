package com.app.backend.product.query;

import com.app.backend.common.exception.BusinessException;
import org.springframework.data.domain.Sort;

import java.util.Locale;
import java.util.Map;

/**
 * Parse sort param dạng <trường>,<hướng> theo whitelist (D-2-06).
 * Luôn thêm khóa phụ id để phân trang ổn định khi giá trị trường trùng.
 */
public final class ProductSort {

    private static final Map<String, String> ALLOWED_FIELDS = Map.of(
            "price", "asc",
            "name", "asc",
            "createdAt", "desc");

    private ProductSort() {
    }

    public static Sort parse(String sortParam) {
        String raw = (sortParam == null || sortParam.isBlank()) ? "createdAt,desc" : sortParam.trim();
        String[] parts = raw.split(",");
        String field = parts[0].trim();

        String defaultDirection = ALLOWED_FIELDS.get(field);
        if (defaultDirection == null) {
            throw BusinessException.validation(
                    "sort field must be one of price, name, createdAt",
                    Map.of("sort", "sort field must be one of price, name, createdAt"));
        }

        String direction = defaultDirection;
        if (parts.length > 1) {
            direction = parts[1].trim().toLowerCase(Locale.ROOT);
            if (!direction.equals("asc") && !direction.equals("desc")) {
                throw BusinessException.validation(
                        "sort direction must be asc or desc",
                        Map.of("sort", "sort direction must be asc or desc"));
            }
            if (parts.length > 2) {
                throw BusinessException.validation(
                        "sort must be <field> or <field>,<direction>",
                        Map.of("sort", "sort must be <field> or <field>,<direction>"));
            }
        }

        Sort.Direction dir = Sort.Direction.fromString(direction);
        return Sort.by(Sort.Order.by(field).with(dir), Sort.Order.desc("id"));
    }
}
