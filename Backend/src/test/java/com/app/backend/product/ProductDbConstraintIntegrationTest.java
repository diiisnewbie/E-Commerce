package com.app.backend.product;

import com.app.backend.common.CatalogIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TC-DB-01: JdbcTemplate insert trực tiếp vi phạm CHECK — DB từ chối (phòng thủ lớp 2).
 */
class ProductDbConstraintIntegrationTest extends CatalogIntegrationTestBase {

    @Test
    void TC_DB_01_check_constraints_tu_choi() {
        String token = adminToken();
        long catId = idOf(mustCreateCategory(token, "DB Cat " + uniqueSuffix(), "desc"));

        // price = 0 vi phạm ck_products_price_positive
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO products (sku, name, description, price, status, category_id, version, created_at, updated_at) "
                        + "VALUES (?, 'Db Test', 'x', 0, 'ACTIVE', ?, 0, NOW(), NOW())",
                "DBCHK-P" + uniqueSuffix(), catId))
                .isInstanceOf(DataIntegrityViolationException.class);

        // sku chữ thường vi phạm ck_products_sku_uppercase
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO products (sku, name, description, price, status, category_id, version, created_at, updated_at) "
                        + "VALUES (?, 'Db Test', 'x', 1000, 'ACTIVE', ?, 0, NOW(), NOW())",
                "dbchk-lower-" + uniqueSuffix(), catId))
                .isInstanceOf(DataIntegrityViolationException.class);

        // quantity = -1 vi phạm ck_inventory_quantity_non_negative
        long productId = jdbcTemplate.queryForObject(
                "INSERT INTO products (sku, name, description, price, status, category_id, version, created_at, updated_at) "
                        + "VALUES (?, 'Db Test Inv', 'x', ?, 'ACTIVE', ?, 0, NOW(), NOW()) RETURNING id",
                Long.class, "DBCHK-I" + uniqueSuffix(), new BigDecimal("1000"), catId);
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO product_inventory (product_id, quantity, updated_at) VALUES (?, -1, NOW())",
                productId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
