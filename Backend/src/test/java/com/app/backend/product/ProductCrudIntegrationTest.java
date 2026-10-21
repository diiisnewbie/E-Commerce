package com.app.backend.product;

import com.app.backend.common.CatalogIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

class ProductCrudIntegrationTest extends CatalogIntegrationTestBase {

    private long createTestCategory(String token) {
        return idOf(mustCreateCategory(token, "Cat PRD " + uniqueSuffix(), "desc"));
    }

    @Test
    void TC_PRD_01_admin_tao_product_hop_le() {
        String token = adminToken();
        long catId = createTestCategory(token);

        Map<String, Object> product = mustCreateProduct(token, "prd01-" + uniqueSuffix(),
                "San pham TC-PRD-01", new BigDecimal("100000"), catId, 30);

        assertThat(product.get("sku").toString()).isUpperCase();
        assertThat(((Number) product.get("stock")).longValue()).isEqualTo(30);
        assertThat(((Number) product.get("version")).longValue()).isZero();
        assertThat(product.get("status")).isEqualTo("ACTIVE");

        // Xuất hiện ngay trong admin list (search admin khớp name + sku)
        ResponseEntity<Map<String, Object>> list = getWithBearer("/api/v1/admin/products?search=" + product.get("sku"), token);
        assertThat(contentOf(list)).anyMatch(p -> p.get("sku").equals(product.get("sku")));
    }

    @Test
    void TC_PRD_02_sku_trung_khac_hoa_thuong() {
        String token = adminToken();
        long catId = createTestCategory(token);
        String sku = "prd02-" + uniqueSuffix();
        mustCreateProduct(token, sku, "Sp A", new BigDecimal("10000"), catId, 1);

        ResponseEntity<Map<String, Object>> duplicate = postWithBearer("/api/v1/admin/products",
                Map.of("sku", sku.toUpperCase(), "name", "Sp B", "price", new BigDecimal("20000"),
                        "categoryId", catId, "initialStock", 1), token);
        assertThat(duplicate.getStatusCode().value()).isEqualTo(409);
        assertThat(errorCodeOf(duplicate)).isEqualTo("SKU_ALREADY_EXISTS");
    }

    @Test
    void TC_PRD_03_price_va_stock_am() {
        String token = adminToken();
        long catId = createTestCategory(token);

        ResponseEntity<Map<String, Object>> zero = postWithBearer("/api/v1/admin/products",
                Map.of("sku", "prd03z-" + uniqueSuffix(), "name", "Sp", "price", BigDecimal.ZERO,
                        "categoryId", catId, "initialStock", 1), token);
        assertThat(zero.getStatusCode().value()).isEqualTo(400);

        ResponseEntity<Map<String, Object>> negative = postWithBearer("/api/v1/admin/products",
                Map.of("sku", "prd03n-" + uniqueSuffix(), "name", "Sp", "price", new BigDecimal("-1"),
                        "categoryId", catId, "initialStock", 1), token);
        assertThat(negative.getStatusCode().value()).isEqualTo(400);

        ResponseEntity<Map<String, Object>> negStock = postWithBearer("/api/v1/admin/products",
                Map.of("sku", "prd03s-" + uniqueSuffix(), "name", "Sp", "price", new BigDecimal("1000"),
                        "categoryId", catId, "initialStock", -1), token);
        assertThat(negStock.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void TC_PRD_04_category_khong_ton_tai() {
        String token = adminToken();

        ResponseEntity<Map<String, Object>> response = postWithBearer("/api/v1/admin/products",
                Map.of("sku", "prd04-" + uniqueSuffix(), "name", "Sp", "price", new BigDecimal("1000"),
                        "categoryId", 999999, "initialStock", 1), token);
        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(errorCodeOf(response)).isEqualTo("CATEGORY_NOT_FOUND");

        // Không tạo được product nào
        String sku = "prd04-" + uniqueSuffix();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM products WHERE sku = ?", Long.class, sku)).isZero();
    }

    @Test
    void TC_PRD_10_admin_put_product_va_category_khong_ton_tai() {
        String token = adminToken();
        long catId = createTestCategory(token);
        long newCatId = createTestCategory(token);
        Map<String, Object> product = mustCreateProduct(token, "prd10-" + uniqueSuffix(),
                "Sp TC-PRD-10", new BigDecimal("50000"), catId, 5);
        long id = idOf(product);

        ResponseEntity<Map<String, Object>> updated = putWithBearer("/api/v1/admin/products/" + id,
                Map.of("name", "Sp TC-PRD-10 moi", "description", "desc", "price", new BigDecimal("60000"),
                        "categoryId", newCatId, "status", "ACTIVE",
                        "version", ((Number) product.get("version")).longValue()), token);
        assertThat(updated.getStatusCode().value()).isEqualTo(200);
        assertThat(((Number) dataOf(updated).get("categoryId")).longValue()).isEqualTo(newCatId);

        ResponseEntity<Map<String, Object>> badCategory = putWithBearer("/api/v1/admin/products/" + id,
                Map.of("name", "Sp", "description", "desc", "price", new BigDecimal("60000"),
                        "categoryId", 999999, "status", "ACTIVE",
                        "version", ((Number) dataOf(updated).get("version")).longValue()), token);
        assertThat(badCategory.getStatusCode().value()).isEqualTo(404);
        assertThat(errorCodeOf(badCategory)).isEqualTo("CATEGORY_NOT_FOUND");
    }

    @Test
    void TC_PRD_11_chuyen_inactive_va_quay_lai_active() {
        String token = adminToken();
        long catId = createTestCategory(token);
        Map<String, Object> product = mustCreateProduct(token, "prd11-" + uniqueSuffix(),
                "Sp TC-PRD-11", new BigDecimal("70000"), catId, 2);
        long id = idOf(product);

        // PATCH sang INACTIVE
        ResponseEntity<Map<String, Object>> inactive = patchWithBearer(
                "/api/v1/admin/products/" + id + "/status", Map.of("status", "INACTIVE"), token);
        assertThat(inactive.getStatusCode().value()).isEqualTo(200);
        assertThat(dataOf(inactive).get("status")).isEqualTo("INACTIVE");

        // Public detail → 404
        ResponseEntity<Map<String, Object>> publicDetail = getPublic("/api/v1/products/" + id);
        assertThat(publicDetail.getStatusCode().value()).isEqualTo(404);
        assertThat(errorCodeOf(publicDetail)).isEqualTo("PRODUCT_NOT_FOUND");

        // Admin vẫn thấy
        ResponseEntity<Map<String, Object>> adminDetail = getWithBearer("/api/v1/admin/products/" + id, token);
        assertThat(adminDetail.getStatusCode().value()).isEqualTo(200);

        // PATCH lại ACTIVE
        ResponseEntity<Map<String, Object>> active = patchWithBearer(
                "/api/v1/admin/products/" + id + "/status", Map.of("status", "ACTIVE"), token);
        assertThat(active.getStatusCode().value()).isEqualTo(200);
        assertThat(getPublic("/api/v1/products/" + id).getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void TC_PRD_12_user_post_admin_products() {
        String email = uniqueEmail("prduser");
        mustRegister(email, "MatKhau123");
        String userToken = accessTokenOf(mustLogin(email, "MatKhau123"));

        ResponseEntity<Map<String, Object>> forbidden = postWithBearer("/api/v1/admin/products",
                Map.of("sku", "prd12-" + uniqueSuffix(), "name", "Sp", "price", new BigDecimal("1000"),
                        "categoryId", 1, "initialStock", 1), userToken);
        assertThat(forbidden.getStatusCode().value()).isEqualTo(403);

        var noToken = restTemplate.exchange("/api/v1/admin/products",
                org.springframework.http.HttpMethod.POST,
                new org.springframework.http.HttpEntity<>(
                        Map.of("sku", "x", "name", "x", "price", new BigDecimal("1"),
                                "categoryId", 1, "initialStock", 1), jsonHeaders()),
                mapType());
        assertThat(noToken.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void TC_PRD_21_put_sku_bi_bo_qua() {
        String token = adminToken();
        long catId = createTestCategory(token);
        String sku = "prd21-" + uniqueSuffix();
        Map<String, Object> product = mustCreateProduct(token, sku, "Sp TC-PRD-21",
                new BigDecimal("10000"), catId, 1);
        long id = idOf(product);

        ResponseEntity<Map<String, Object>> response = putWithBearer("/api/v1/admin/products/" + id,
                Map.of("name", "Sp TC-PRD-21 doi", "description", "desc", "price", new BigDecimal("11000"),
                        "categoryId", catId, "status", "ACTIVE",
                        "version", ((Number) product.get("version")).longValue()), token);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(dataOf(response).get("sku")).isEqualTo(sku.toUpperCase());
    }

    @Test
    void TC_PRD_22_put_enum_sai_va_thieu_truong() {
        String token = adminToken();
        long catId = createTestCategory(token);
        Map<String, Object> product = mustCreateProduct(token, "prd22-" + uniqueSuffix(),
                "Sp TC-PRD-22", new BigDecimal("10000"), catId, 1);
        long id = idOf(product);
        long version = ((Number) product.get("version")).longValue();

        ResponseEntity<Map<String, Object>> badEnum = putWithBearer("/api/v1/admin/products/" + id,
                Map.of("name", "Sp", "description", "d", "price", new BigDecimal("1"),
                        "categoryId", catId, "status", "DELETED", "version", version), token);
        assertThat(badEnum.getStatusCode().value()).isEqualTo(400);

        ResponseEntity<Map<String, Object>> badPrice = putWithBearer("/api/v1/admin/products/" + id,
                Map.of("name", "Sp", "description", "d", "price", BigDecimal.ZERO,
                        "categoryId", catId, "status", "ACTIVE", "version", version), token);
        assertThat(badPrice.getStatusCode().value()).isEqualTo(400);

        ResponseEntity<Map<String, Object>> missingName = putWithBearer("/api/v1/admin/products/" + id,
                Map.of("description", "d", "price", new BigDecimal("1"),
                        "categoryId", catId, "status", "ACTIVE", "version", version), token);
        assertThat(missingName.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void TC_PRD_23_admin_id_khong_ton_tai() {
        String token = adminToken();

        ResponseEntity<Map<String, Object>> get = getWithBearer("/api/v1/admin/products/999999", token);
        assertThat(get.getStatusCode().value()).isEqualTo(404);
        assertThat(errorCodeOf(get)).isEqualTo("PRODUCT_NOT_FOUND");

        ResponseEntity<Map<String, Object>> put = putWithBearer("/api/v1/admin/products/999999",
                Map.of("name", "Sp", "description", "d", "price", new BigDecimal("1"),
                        "categoryId", 1, "status", "ACTIVE", "version", 0), token);
        assertThat(put.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void TC_PRD_24_sku_sai_dinh_dang() {
        String token = adminToken();
        long catId = createTestCategory(token);

        ResponseEntity<Map<String, Object>> tooShort = postWithBearer("/api/v1/admin/products",
                Map.of("sku", "ab", "name", "Sp", "price", new BigDecimal("1000"),
                        "categoryId", catId, "initialStock", 1), token);
        assertThat(tooShort.getStatusCode().value()).isEqualTo(400);

        ResponseEntity<Map<String, Object>> withSpace = postWithBearer("/api/v1/admin/products",
                Map.of("sku", "ab cd", "name", "Sp", "price", new BigDecimal("1000"),
                        "categoryId", catId, "initialStock", 1), token);
        assertThat(withSpace.getStatusCode().value()).isEqualTo(400);

        ResponseEntity<Map<String, Object>> special = postWithBearer("/api/v1/admin/products",
                Map.of("sku", "ab@cd!", "name", "Sp", "price", new BigDecimal("1000"),
                        "categoryId", catId, "initialStock", 1), token);
        assertThat(special.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void TC_PRD_25_hai_request_song_song_cung_sku() throws Exception {
        String token = adminToken();
        long catId = createTestCategory(token);
        String sku = "prd25-" + uniqueSuffix();
        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> f1 = pool.submit(() -> {
                barrier.await();
                return postWithBearer("/api/v1/admin/products",
                        Map.of("sku", sku, "name", "Sp A", "price", new BigDecimal("1000"),
                                "categoryId", catId, "initialStock", 1), token).getStatusCode().value();
            });
            Future<Integer> f2 = pool.submit(() -> {
                barrier.await();
                return postWithBearer("/api/v1/admin/products",
                        Map.of("sku", sku, "name", "Sp B", "price", new BigDecimal("2000"),
                                "categoryId", catId, "initialStock", 1), token).getStatusCode().value();
            });
            int s1 = f1.get();
            int s2 = f2.get();
            assertThat(s1).isIn(201, 409);
            assertThat(s2).isIn(201, 409);
            assertThat(s1 == 201 || s2 == 201).isTrue();
            assertThat(s1 == 409 || s2 == 409).isTrue();
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void TC_PRD_29_updated_at_doi_created_at_giu_nguyen() throws Exception {
        String token = adminToken();
        long catId = createTestCategory(token);
        Map<String, Object> product = mustCreateProduct(token, "prd29-" + uniqueSuffix(),
                "Sp TC-PRD-29", new BigDecimal("10000"), catId, 1);
        long id = idOf(product);
        // createdAt từ create response là in-memory Instant (nanos); từ PUT response là DB micros — so sánh bằng Instant
        java.time.Instant createdAt = java.time.Instant.parse((String) product.get("createdAt"));
        java.time.Instant updatedAtBefore = java.time.Instant.parse((String) product.get("updatedAt"));

        Thread.sleep(10); // đảm bảo clock nhảy khác nhau

        ResponseEntity<Map<String, Object>> updated = putWithBearer("/api/v1/admin/products/" + id,
                Map.of("name", "Sp TC-PRD-29 moi", "description", "desc", "price", new BigDecimal("12000"),
                        "categoryId", catId, "status", "ACTIVE",
                        "version", ((Number) product.get("version")).longValue()), token);
        assertThat(updated.getStatusCode().value()).isEqualTo(200);
        // createdAt giữ nguyên (DB truncate micros — chênh lệch so với in-memory Instant < 1ms)
        java.time.Instant createdAtAfter = java.time.Instant.parse((String) dataOf(updated).get("createdAt"));
        assertThat(Math.abs(createdAtAfter.toEpochMilli() - createdAt.toEpochMilli())).isLessThan(1);
        java.time.Instant updatedAfter = java.time.Instant.parse((String) dataOf(updated).get("updatedAt"));
        assertThat(updatedAfter).isAfter(updatedAtBefore);
    }

    @Test
    void TC_PRD_30_post_status_va_id_bi_bo_qua() {
        String token = adminToken();
        long catId = createTestCategory(token);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange("/api/v1/admin/products",
                org.springframework.http.HttpMethod.POST,
                new org.springframework.http.HttpEntity<>(Map.of(
                        "sku", "prd30-" + uniqueSuffix(),
                        "name", "Sp TC-PRD-30",
                        "price", new BigDecimal("10000"),
                        "categoryId", catId,
                        "initialStock", 5,
                        "status", "INACTIVE",
                        "id", 12345), bearerHeaders(token)),
                mapType());
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(dataOf(response).get("status")).isEqualTo("ACTIVE");
        assertThat(((Number) dataOf(response).get("id")).longValue()).isNotEqualTo(12345);
    }

    @Test
    void TC_PRD_31_get_admin_by_id() {
        String token = adminToken();
        long catId = createTestCategory(token);
        Map<String, Object> product = mustCreateProduct(token, "prd31-" + uniqueSuffix(),
                "Sp TC-PRD-31", new BigDecimal("10000"), catId, 7);
        long id = idOf(product);

        ResponseEntity<Map<String, Object>> response = getWithBearer("/api/v1/admin/products/" + id, token);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(((Number) dataOf(response).get("stock")).longValue()).isEqualTo(7);
        assertThat(dataOf(response)).containsKey("version");

        ResponseEntity<Map<String, Object>> missing = getWithBearer("/api/v1/admin/products/999999", token);
        assertThat(missing.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void TC_PRD_32_patch_status_idempotent() {
        String token = adminToken();
        long catId = createTestCategory(token);
        Map<String, Object> product = mustCreateProduct(token, "prd32-" + uniqueSuffix(),
                "Sp TC-PRD-32", new BigDecimal("10000"), catId, 1);
        long id = idOf(product);
        long versionBefore = ((Number) product.get("version")).longValue();

        // Đổi thật → version +1, trả AdminProductResponse đầy đủ
        ResponseEntity<Map<String, Object>> changed = patchWithBearer(
                "/api/v1/admin/products/" + id + "/status", Map.of("status", "INACTIVE"), token);
        assertThat(changed.getStatusCode().value()).isEqualTo(200);
        assertThat(dataOf(changed)).containsKeys("sku", "status", "stock", "version", "categoryId");
        long versionAfter = ((Number) dataOf(changed).get("version")).longValue();
        assertThat(versionAfter).isEqualTo(versionBefore + 1);

        // No-op → 200, version giữ nguyên
        ResponseEntity<Map<String, Object>> noop = patchWithBearer(
                "/api/v1/admin/products/" + id + "/status", Map.of("status", "INACTIVE"), token);
        assertThat(noop.getStatusCode().value()).isEqualTo(200);
        assertThat(((Number) dataOf(noop).get("version")).longValue()).isEqualTo(versionAfter);

        // Sai enum → 400 fields.status
        ResponseEntity<Map<String, Object>> badEnum = patchWithBearer(
                "/api/v1/admin/products/" + id + "/status", Map.of("status", "BOGUS"), token);
        assertThat(badEnum.getStatusCode().value()).isEqualTo(400);
        assertThat(errorCodeOf(badEnum)).isEqualTo("VALIDATION_ERROR");
        assertThat(fieldsOf(badEnum)).containsKey("status");

        // Id không tồn tại → 404
        ResponseEntity<Map<String, Object>> missing = patchWithBearer(
                "/api/v1/admin/products/999999/status", Map.of("status", "ACTIVE"), token);
        assertThat(missing.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void TC_PRD_33_version_round_trip() {
        String token = adminToken();
        long catId = createTestCategory(token);
        Map<String, Object> product = mustCreateProduct(token, "prd33-" + uniqueSuffix(),
                "Sp TC-PRD-33", new BigDecimal("10000"), catId, 1);
        long id = idOf(product);
        long version = ((Number) product.get("version")).longValue();

        // PUT version đúng → 200 version +1
        ResponseEntity<Map<String, Object>> ok = putWithBearer("/api/v1/admin/products/" + id,
                Map.of("name", "Sp TC-PRD-33 moi", "description", "d", "price", new BigDecimal("11000"),
                        "categoryId", catId, "status", "ACTIVE", "version", version), token);
        assertThat(ok.getStatusCode().value()).isEqualTo(200);
        long newVersion = ((Number) dataOf(ok).get("version")).longValue();
        assertThat(newVersion).isEqualTo(version + 1);

        // PUT lại version cũ → 409 CONCURRENT_MODIFICATION
        ResponseEntity<Map<String, Object>> stale = putWithBearer("/api/v1/admin/products/" + id,
                Map.of("name", "Sp TC-PRD-33 cu", "description", "d", "price", new BigDecimal("11000"),
                        "categoryId", catId, "status", "ACTIVE", "version", version), token);
        assertThat(stale.getStatusCode().value()).isEqualTo(409);
        assertThat(errorCodeOf(stale)).isEqualTo("CONCURRENT_MODIFICATION");

        // PUT thiếu version → 400
        ResponseEntity<Map<String, Object>> missingVersion = putWithBearer("/api/v1/admin/products/" + id,
                Map.of("name", "Sp", "description", "d", "price", new BigDecimal("11000"),
                        "categoryId", catId, "status", "ACTIVE"), token);
        assertThat(missingVersion.getStatusCode().value()).isEqualTo(400);
    }
}
