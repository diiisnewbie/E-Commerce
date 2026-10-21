package com.app.backend.product;

import com.app.backend.common.CatalogIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ProductListIntegrationTest extends CatalogIntegrationTestBase {

    private long createTestCategory(String token) {
        return idOf(mustCreateCategory(token, "List Cat " + uniqueSuffix(), "desc"));
    }

    @Test
    void TC_PRD_05_public_list_mac_dinh() {
        String token = adminToken();
        long catId = createTestCategory(token);
        String prefix = "prd05" + uniqueSuffix();
        mustCreateProduct(token, prefix + "-A", "Sp A " + prefix, new BigDecimal("10000"), catId, 1);
        mustCreateProduct(token, prefix + "-B", "Sp B " + prefix, new BigDecimal("20000"), catId, 1);

        ResponseEntity<Map<String, Object>> list = getPublic("/api/v1/products?search=" + prefix);
        assertThat(list.getStatusCode().value()).isEqualTo(200);
        List<Map<String, Object>> content = contentOf(list);
        assertThat(content).hasSize(2);
        // createdAt desc — sp tạo sau đứng trước (dùng id tiebreaker sort createdAt,desc mặc định)
        assertThat(content.get(0).get("createdAt").toString())
                .isGreaterThanOrEqualTo(content.get(1).get("createdAt").toString());
        // Không có stock/status
        assertThat(content.get(0)).doesNotContainKeys("stock", "status");
    }

    @Test
    void TC_PRD_06_filter_category_price_search() {
        String token = adminToken();
        long catA = createTestCategory(token);
        long catB = createTestCategory(token);
        String prefix = "prd06" + uniqueSuffix();
        mustCreateProduct(token, prefix + "-1", "Alpha " + prefix, new BigDecimal("10000"), catA, 1);
        mustCreateProduct(token, prefix + "-2", "beta " + prefix, new BigDecimal("50000"), catA, 1);
        mustCreateProduct(token, prefix + "-3", "Gamma Other", new BigDecimal("30000"), catB, 1);

        // Filter categoryId
        ResponseEntity<Map<String, Object>> byCat = getPublic(
                "/api/v1/products?categoryId=" + catA + "&search=" + prefix);
        assertThat(contentOf(byCat)).hasSize(2);

        // Khoảng giá (AND)
        ResponseEntity<Map<String, Object>> byPrice = getPublic(
                "/api/v1/products?categoryId=" + catA + "&minPrice=15000&maxPrice=60000&search=" + prefix);
        assertThat(contentOf(byPrice)).hasSize(1);
        assertThat(contentOf(byPrice).get(0).get("name").toString()).contains("beta");

        // Search HOA/thuong
        ResponseEntity<Map<String, Object>> upper = getPublic(
                "/api/v1/products?search=" + prefix.toUpperCase());
        assertThat(contentOf(upper)).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void TC_PRD_07_sort_hop_le_va_loi() {
        String token = adminToken();
        long catId = createTestCategory(token);
        String prefix = "prd07" + uniqueSuffix();
        mustCreateProduct(token, prefix + "-1", "Sp1 " + prefix, new BigDecimal("30000"), catId, 1);
        mustCreateProduct(token, prefix + "-2", "Sp2 " + prefix, new BigDecimal("10000"), catId, 1);
        mustCreateProduct(token, prefix + "-3", "Sp3 " + prefix, new BigDecimal("20000"), catId, 1);

        ResponseEntity<Map<String, Object>> priceAsc = getPublic(
                "/api/v1/products?search=" + prefix + "&sort=price,asc");
        List<Map<String, Object>> sorted = contentOf(priceAsc);
        assertThat(sorted).hasSize(3);
        assertThat(new BigDecimal(sorted.get(0).get("price").toString()))
                .isLessThanOrEqualTo(new BigDecimal(sorted.get(1).get("price").toString()));

        ResponseEntity<Map<String, Object>> nameDesc = getPublic(
                "/api/v1/products?search=" + prefix + "&sort=name,desc");
        assertThat(nameDesc.getStatusCode().value()).isEqualTo(200);

        ResponseEntity<Map<String, Object>> badField = getPublic("/api/v1/products?sort=password,asc");
        assertThat(badField.getStatusCode().value()).isEqualTo(400);
        assertThat(errorCodeOf(badField)).isEqualTo("VALIDATION_ERROR");
        assertThat(fieldsOf(badField)).containsKey("sort");

        ResponseEntity<Map<String, Object>> badDir = getPublic("/api/v1/products?sort=price,sideways");
        assertThat(badDir.getStatusCode().value()).isEqualTo(400);
        assertThat(fieldsOf(badDir)).containsKey("sort");
    }

    @Test
    void TC_PRD_08_public_detail_active_inactive_khong_ton_tai() {
        String token = adminToken();
        long catId = createTestCategory(token);
        Map<String, Object> active = mustCreateProduct(token, "prd08a-" + uniqueSuffix(),
                "Active Sp", new BigDecimal("10000"), catId, 1);
        Map<String, Object> inactive = mustCreateProduct(token, "prd08i-" + uniqueSuffix(),
                "Inactive Sp", new BigDecimal("10000"), catId, 1);
        long inactiveId = idOf(inactive);
        patchWithBearer("/api/v1/admin/products/" + inactiveId + "/status",
                Map.of("status", "INACTIVE"), token);

        assertThat(getPublic("/api/v1/products/" + idOf(active)).getStatusCode().value()).isEqualTo(200);

        ResponseEntity<Map<String, Object>> inactiveDetail = getPublic("/api/v1/products/" + inactiveId);
        assertThat(inactiveDetail.getStatusCode().value()).isEqualTo(404);
        assertThat(errorCodeOf(inactiveDetail)).isEqualTo("PRODUCT_NOT_FOUND");

        ResponseEntity<Map<String, Object>> missing = getPublic("/api/v1/products/999999");
        assertThat(missing.getStatusCode().value()).isEqualTo(404);
        assertThat(errorCodeOf(missing)).isEqualTo("PRODUCT_NOT_FOUND");
    }

    @Test
    void TC_PRD_09_admin_list_thay_inactive_stock_version() {
        String token = adminToken();
        long catId = createTestCategory(token);
        String prefix = "prd09" + uniqueSuffix();
        Map<String, Object> p1 = mustCreateProduct(token, prefix + "-1", "Sp1 " + prefix,
                new BigDecimal("10000"), catId, 3);
        Map<String, Object> p2 = mustCreateProduct(token, prefix + "-2", "Sp2 " + prefix,
                new BigDecimal("20000"), catId, 4);
        patchWithBearer("/api/v1/admin/products/" + idOf(p2) + "/status",
                Map.of("status", "INACTIVE"), token);

        // Không filter status → thấy cả hai
        ResponseEntity<Map<String, Object>> all = getWithBearer(
                "/api/v1/admin/products?search=" + prefix, token);
        List<Map<String, Object>> content = contentOf(all);
        assertThat(content).hasSize(2);
        assertThat(content.get(0)).containsKeys("stock", "version", "status");

        // Filter status=INACTIVE
        ResponseEntity<Map<String, Object>> inactiveOnly = getWithBearer(
                "/api/v1/admin/products?search=" + prefix + "&status=INACTIVE", token);
        assertThat(contentOf(inactiveOnly)).hasSize(1);
        assertThat(contentOf(inactiveOnly).get(0).get("status")).isEqualTo("INACTIVE");
        assertThat(((Number) contentOf(inactiveOnly).get(0).get("stock")).longValue()).isEqualTo(4);
        assertThat(((Number) contentOf(inactiveOnly).get(0).get("version")).longValue()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void TC_PRD_13_size_va_page_contract() {
        String token = adminToken();

        ResponseEntity<Map<String, Object>> tooBig = getPublic("/api/v1/products?size=51");
        assertThat(tooBig.getStatusCode().value()).isEqualTo(400);
        assertThat(errorCodeOf(tooBig)).isEqualTo("VALIDATION_ERROR");

        ResponseEntity<Map<String, Object>> maxSize = getPublic("/api/v1/products?size=50");
        assertThat(maxSize.getStatusCode().value()).isEqualTo(200);

        ResponseEntity<Map<String, Object>> farPage = getPublic("/api/v1/products?page=999");
        assertThat(farPage.getStatusCode().value()).isEqualTo(200);
        assertThat(contentOf(farPage)).isEmpty();
    }

    @Test
    void TC_PRD_14_min_loin_max() {
        ResponseEntity<Map<String, Object>> response = getPublic("/api/v1/products?minPrice=100&maxPrice=50");
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(errorCodeOf(response)).isEqualTo("VALIDATION_ERROR");
        assertThat(fieldsOf(response)).containsKey("minPrice");
    }

    @Test
    void TC_PRD_15_search_khong_ton_tai() {
        ResponseEntity<Map<String, Object>> response = getPublic("/api/v1/products?search=khongtantai99999");
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(contentOf(response)).isEmpty();
    }

    @Test
    void TC_PRD_16_public_khong_lo_stock_status() {
        String token = adminToken();
        long catId = createTestCategory(token);
        String prefix = "prd16" + uniqueSuffix();
        Map<String, Object> product = mustCreateProduct(token, prefix, "Sp16 " + prefix,
                new BigDecimal("10000"), catId, 5);
        long id = idOf(product);

        ResponseEntity<Map<String, Object>> list = getPublic("/api/v1/products?search=Sp16 " + prefix);
        assertThat(contentOf(list).get(0)).doesNotContainKeys("stock", "status");

        ResponseEntity<Map<String, Object>> detail = getPublic("/api/v1/products/" + id);
        assertThat(dataOf(detail)).doesNotContainKeys("stock", "status");
    }

    @Test
    void TC_PRD_17_public_status_bi_bo_qua() {
        String token = adminToken();
        long catId = createTestCategory(token);
        String prefix = "prd17" + uniqueSuffix();
        Map<String, Object> active = mustCreateProduct(token, prefix + "-A", "Active " + prefix,
                new BigDecimal("10000"), catId, 1);
        Map<String, Object> inactive = mustCreateProduct(token, prefix + "-I", "Inactive " + prefix,
                new BigDecimal("10000"), catId, 1);
        patchWithBearer("/api/v1/admin/products/" + idOf(inactive) + "/status",
                Map.of("status", "INACTIVE"), token);

        ResponseEntity<Map<String, Object>> withStatus = getPublic(
                "/api/v1/products?search=" + prefix + "&status=INACTIVE");
        assertThat(withStatus.getStatusCode().value()).isEqualTo(200);
        List<Map<String, Object>> content = contentOf(withStatus);
        // Chỉ ACTIVE — param bị bỏ qua hoàn toàn
        assertThat(content).hasSize(1);
        assertThat(((Number) content.get(0).get("id")).longValue()).isEqualTo(idOf(active));

        ResponseEntity<Map<String, Object>> activeParam = getPublic(
                "/api/v1/products?search=" + prefix + "&status=ACTIVE");
        assertThat(activeParam.getStatusCode().value()).isEqualTo(200);
        assertThat(contentOf(activeParam)).hasSize(1);
    }

    @Test
    void TC_PRD_18_phan_trang_on_dinh_khoa_phu_id() {
        String token = adminToken();
        long catId = createTestCategory(token);
        String prefix = "prd18" + uniqueSuffix();
        // 5 sp cùng giá — sort price,asc không ổn định nếu thiếu id tiebreaker
        for (int i = 1; i <= 5; i++) {
            mustCreateProduct(token, prefix + "-" + i, "Sp" + i + " " + prefix,
                    new BigDecimal("50000"), catId, 1);
        }

        Set<Long> allIds = new HashSet<>();
        for (int page = 0; page < 5; page++) {
            ResponseEntity<Map<String, Object>> response = getPublic(
                    "/api/v1/products?search=" + prefix + "&size=2&sort=price,asc&page=" + page);
            assertThat(response.getStatusCode().value()).isEqualTo(200);
            List<Map<String, Object>> content = contentOf(response);
            for (Map<String, Object> p : content) {
                allIds.add(((Number) p.get("id")).longValue());
            }
        }
        // Hợp các trang = đúng 5, không lặp (Set), không sót
        assertThat(allIds).hasSize(5);
    }

    @Test
    void TC_PRD_19_price_qua_precision() {
        String token = adminToken();
        long catId = createTestCategory(token);

        ResponseEntity<Map<String, Object>> fraction = postWithBearer("/api/v1/admin/products",
                Map.of("sku", "prd19a-" + uniqueSuffix(), "name", "Sp", "price", new BigDecimal("100.555"),
                        "categoryId", catId, "initialStock", 1), token);
        assertThat(fraction.getStatusCode().value()).isEqualTo(400);

        ResponseEntity<Map<String, Object>> tooManyDigits = postWithBearer("/api/v1/admin/products",
                Map.of("sku", "prd19b-" + uniqueSuffix(), "name", "Sp",
                        "price", new BigDecimal("123456789012345678"), // 18 chữ số nguyên
                        "categoryId", catId, "initialStock", 1), token);
        assertThat(tooManyDigits.getStatusCode().value()).isEqualTo(400);

        // price quá lớn dạng scientific → 400, không lưu được giá đã làm tròn
        var sciResponse = restTemplate.exchange("/api/v1/admin/products",
                org.springframework.http.HttpMethod.POST,
                new org.springframework.http.HttpEntity<>(
                        "{\"sku\":\"prd19c-" + uniqueSuffix() + "\",\"name\":\"Sp\",\"price\":1e30,"
                                + "\"categoryId\":" + catId + ",\"initialStock\":1}",
                        bearerHeaders(token)),
                mapType());
        assertThat(sciResponse.getStatusCode().value()).isEqualTo(400);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM products WHERE sku LIKE 'prd19c-%'", Long.class)).isZero();
    }

    @Test
    void TC_PRD_20_query_sai_kieu_fields_key() {
        ResponseEntity<Map<String, Object>> sizeAbc = getPublic("/api/v1/products?size=abc");
        assertThat(sizeAbc.getStatusCode().value()).isEqualTo(400);
        assertThat(errorCodeOf(sizeAbc)).isEqualTo("VALIDATION_ERROR");
        assertThat(fieldsOf(sizeAbc)).containsKey("size");

        ResponseEntity<Map<String, Object>> pageNeg = getPublic("/api/v1/products?page=-1");
        assertThat(pageNeg.getStatusCode().value()).isEqualTo(400);
        assertThat(fieldsOf(pageNeg)).containsKey("page");

        ResponseEntity<Map<String, Object>> catAbc = getPublic("/api/v1/products?categoryId=abc");
        assertThat(catAbc.getStatusCode().value()).isEqualTo(400);
        assertThat(fieldsOf(catAbc)).containsKey("categoryId");

        ResponseEntity<Map<String, Object>> minAbc = getPublic("/api/v1/products?minPrice=abc");
        assertThat(minAbc.getStatusCode().value()).isEqualTo(400);
        assertThat(fieldsOf(minAbc)).containsKey("minPrice");

        ResponseEntity<Map<String, Object>> badId = getPublic("/api/v1/products/abc");
        assertThat(badId.getStatusCode().value()).isEqualTo(400);
        assertThat(errorCodeOf(badId)).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void TC_PRD_26_search_wildcard_va_blank() {
        String token = adminToken();
        long catId = createTestCategory(token);
        String prefix = "prd26" + uniqueSuffix();
        // Tên chứa ký tự underscore — search "_" không được thành wildcard
        mustCreateProduct(token, prefix + "-1", "Sp_With_Underscore " + prefix,
                new BigDecimal("10000"), catId, 1);
        mustCreateProduct(token, prefix + "-2", "Sp Plain " + prefix, new BigDecimal("10000"), catId, 1);

        // Search "_" chỉ khớp dòng có underscore literal
        ResponseEntity<Map<String, Object>> underscore = getPublic(
                "/api/v1/products?search=Sp_With");
        assertThat(contentOf(underscore))
                .anyMatch(p -> p.get("name").toString().contains("Sp_With_Underscore"))
                .noneMatch(p -> p.get("name").toString().contains("Sp Plain"));

        // Search blank → không lọc
        ResponseEntity<Map<String, Object>> blank = getPublic("/api/v1/products?search=%20%20");
        assertThat(blank.getStatusCode().value()).isEqualTo(200);

        // 101 ký tự → 400
        ResponseEntity<Map<String, Object>> tooLong = getPublic("/api/v1/products?search=" + "x".repeat(101));
        assertThat(tooLong.getStatusCode().value()).isEqualTo(400);
        assertThat(fieldsOf(tooLong)).containsKey("search");
    }

    @Test
    void TC_PRD_27_price_bounds() {
        String token = adminToken();
        long catId = createTestCategory(token);
        String prefix = "prd27" + uniqueSuffix();
        mustCreateProduct(token, prefix + "-1", "Sp1 " + prefix, new BigDecimal("10000"), catId, 1);
        mustCreateProduct(token, prefix + "-2", "Sp2 " + prefix, new BigDecimal("20000"), catId, 1);
        mustCreateProduct(token, prefix + "-3", "Sp3 " + prefix, new BigDecimal("30000"), catId, 1);

        // Chỉ minPrice — bao gồm đầu
        ResponseEntity<Map<String, Object>> minOnly = getPublic(
                "/api/v1/products?search=" + prefix + "&minPrice=20000");
        assertThat(contentOf(minOnly)).hasSize(2);
        assertThat(contentOf(minOnly)).anyMatch(p -> new BigDecimal(p.get("price").toString())
                .compareTo(new BigDecimal("20000")) == 0);

        // Chỉ maxPrice
        ResponseEntity<Map<String, Object>> maxOnly = getPublic(
                "/api/v1/products?search=" + prefix + "&maxPrice=20000");
        assertThat(contentOf(maxOnly)).hasSize(2);

        // min == max — bao gồm hai đầu
        ResponseEntity<Map<String, Object>> equal = getPublic(
                "/api/v1/products?search=" + prefix + "&minPrice=20000&maxPrice=20000");
        assertThat(contentOf(equal)).hasSize(1);
    }

    @Test
    void TC_PRD_28_catalog_rong() {
        String token = adminToken();
        long emptyCatId = createTestCategory(token);

        ResponseEntity<Map<String, Object>> response = getPublic(
                "/api/v1/products?categoryId=" + emptyCatId);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(contentOf(response)).isEmpty();
        Map<String, Object> data = dataOf(response);
        assertThat(((Number) data.get("totalElements")).longValue()).isZero();
        assertThat(((Number) data.get("totalPages")).intValue()).isZero();
        assertThat(data.get("first")).isEqualTo(true);
        assertThat(data.get("last")).isEqualTo(true);
    }
}
