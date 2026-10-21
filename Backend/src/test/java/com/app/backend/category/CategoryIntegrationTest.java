package com.app.backend.category;

import com.app.backend.common.CatalogIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryIntegrationTest extends CatalogIntegrationTestBase {

    @Test
    void TC_CAT_01_admin_tao_category_hop_le() {
        String token = adminToken();
        Map<String, Object> category = mustCreateCategory(token, "Dien thoai " + uniqueSuffix(), "Dien thoai va phu kien");

        assertThat(category.get("name").toString()).isLowerCase();
        assertThat(category.get("description")).isEqualTo("Dien thoai va phu kien");
    }

    @Test
    void TC_CAT_02_tao_trung_name_khac_hoa_thuong() {
        String token = adminToken();
        String name = "Laptop " + uniqueSuffix();
        mustCreateCategory(token, name, "desc");

        ResponseEntity<Map<String, Object>> duplicate = postWithBearer("/api/v1/admin/categories",
                Map.of("name", name.toUpperCase(), "description", "desc"), token);
        assertThat(duplicate.getStatusCode().value()).isEqualTo(409);
        assertThat(errorCodeOf(duplicate)).isEqualTo("CATEGORY_NAME_ALREADY_EXISTS");
    }

    @Test
    void TC_CAT_03_name_1_ky_tu_qua_dai_trong() {
        String token = adminToken();

        ResponseEntity<Map<String, Object>> tooShort = postWithBearer("/api/v1/admin/categories",
                Map.of("name", "a", "description", "x"), token);
        assertThat(tooShort.getStatusCode().value()).isEqualTo(400);
        assertThat(errorCodeOf(tooShort)).isEqualTo("VALIDATION_ERROR");

        ResponseEntity<Map<String, Object>> tooLong = postWithBearer("/api/v1/admin/categories",
                Map.of("name", "x".repeat(101), "description", "x"), token);
        assertThat(tooLong.getStatusCode().value()).isEqualTo(400);

        ResponseEntity<Map<String, Object>> blank = postWithBearer("/api/v1/admin/categories",
                Map.of("name", "   ", "description", "x"), token);
        assertThat(blank.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void TC_CAT_04_user_tao_category_bi_tu_choi() {
        String email = uniqueEmail("catuser");
        mustRegister(email, "MatKhau123");
        String userToken = accessTokenOf(mustLogin(email, "MatKhau123"));

        ResponseEntity<Map<String, Object>> forbidden = postWithBearer("/api/v1/admin/categories",
                Map.of("name", "User Cat " + uniqueSuffix(), "description", "x"), userToken);
        assertThat(forbidden.getStatusCode().value()).isEqualTo(403);
        assertThat(errorCodeOf(forbidden)).isEqualTo("ACCESS_DENIED");

        ResponseEntity<Map<String, Object>> unauthorized = restTemplate.exchange("/api/v1/admin/categories",
                HttpMethod.POST, new HttpEntity<>(Map.of("name", "NoToken", "description", "x"), jsonHeaders()), mapType());
        assertThat(unauthorized.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void TC_CAT_05_update_name_hop_le_va_trung_name() {
        String token = adminToken();
        Map<String, Object> category = mustCreateCategory(token, "Phu kien " + uniqueSuffix(), "desc");
        long id = idOf(category);

        ResponseEntity<Map<String, Object>> updated = putWithBearer("/api/v1/admin/categories/" + id,
                Map.of("name", "Phu kien moi " + uniqueSuffix(), "description", "desc2"), token);
        assertThat(updated.getStatusCode().value()).isEqualTo(200);
        assertThat(dataOf(updated).get("name").toString()).isLowerCase();

        String existingName = "Ten Da Ton Tai " + uniqueSuffix();
        mustCreateCategory(token, existingName, "desc");
        ResponseEntity<Map<String, Object>> conflict = putWithBearer("/api/v1/admin/categories/" + id,
                Map.of("name", existingName.toUpperCase(), "description", "desc2"), token);
        assertThat(conflict.getStatusCode().value()).isEqualTo(409);
        assertThat(errorCodeOf(conflict)).isEqualTo("CATEGORY_NAME_ALREADY_EXISTS");
    }

    @Test
    void TC_CAT_06_xoa_category_trong() {
        String token = adminToken();
        Map<String, Object> category = mustCreateCategory(token, "Xoa Trong " + uniqueSuffix(), "desc");
        long id = idOf(category);

        ResponseEntity<Map<String, Object>> deleted = deleteWithBearer("/api/v1/admin/categories/" + id, token);
        assertThat(deleted.getStatusCode().value()).isEqualTo(200);
        assertThat(deleted.getBody().get("data")).isNull();

        ResponseEntity<Map<String, Object>> list = getWithBearer("/api/v1/categories", token);
        java.util.List<Map<String, Object>> items =
                (java.util.List<Map<String, Object>>) list.getBody().get("data");
        assertThat(items).noneMatch(c -> ((Number) c.get("id")).longValue() == id);
    }

    @Test
    void TC_CAT_07_xoa_category_con_product() {
        String token = adminToken();
        Map<String, Object> category = mustCreateCategory(token, "Con Sp " + uniqueSuffix(), "desc");
        long catId = idOf(category);
        mustCreateProduct(token, "CAT07-" + uniqueSuffix(), "Sp TC-CAT-07",
                new java.math.BigDecimal("100000"), catId, 5);

        ResponseEntity<Map<String, Object>> response = deleteWithBearer("/api/v1/admin/categories/" + catId, token);
        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(errorCodeOf(response)).isEqualTo("CATEGORY_HAS_PRODUCTS");
    }

    @Test
    void TC_CAT_08_update_delete_id_khong_ton_tai() {
        String token = adminToken();

        ResponseEntity<Map<String, Object>> update = putWithBearer("/api/v1/admin/categories/999999",
                Map.of("name", "Ghost " + uniqueSuffix(), "description", "x"), token);
        assertThat(update.getStatusCode().value()).isEqualTo(404);
        assertThat(errorCodeOf(update)).isEqualTo("CATEGORY_NOT_FOUND");

        ResponseEntity<Map<String, Object>> delete = deleteWithBearer("/api/v1/admin/categories/999999", token);
        assertThat(delete.getStatusCode().value()).isEqualTo(404);
        assertThat(errorCodeOf(delete)).isEqualTo("CATEGORY_NOT_FOUND");
    }

    @Test
    void TC_CAT_09_public_list_khong_token_sap_xep_name_asc() {
        String token = adminToken();
        mustCreateCategory(token, "Zebra " + uniqueSuffix(), "desc");
        mustCreateCategory(token, "Alpha " + uniqueSuffix(), "desc");

        ResponseEntity<Map<String, Object>> list = restTemplate.exchange("/api/v1/categories",
                HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), mapType());
        assertThat(list.getStatusCode().value()).isEqualTo(200);

        java.util.List<Map<String, Object>> items =
                (java.util.List<Map<String, Object>>) list.getBody().get("data");
        assertThat(items).isNotEmpty();
        java.util.List<String> names = items.stream().map(c -> (String) c.get("name")).toList();
        assertThat(names).isSorted();
    }

    @Test
    void TC_CAT_10_duoc_name_chi_doi_description_khong_409() {
        String token = adminToken();
        String name = "Giu Ten " + uniqueSuffix();
        Map<String, Object> category = mustCreateCategory(token, name, "desc cu");
        long id = idOf(category);

        ResponseEntity<Map<String, Object>> response = putWithBearer("/api/v1/admin/categories/" + id,
                Map.of("name", name, "description", "desc moi"), token);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(dataOf(response).get("description")).isEqualTo("desc moi");
    }

    @Test
    void TC_CAT_11_hai_request_song_song_cung_ten() throws Exception {
        String token = adminToken();
        String name = "Song Song " + uniqueSuffix();
        CyclicBarrier barrier = new CyclicBarrier(2);

        CallableStatusCallable create = new CallableStatusCallable(barrier, token, name);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> f1 = pool.submit(create);
            Future<Integer> f2 = pool.submit(new CallableStatusCallable(barrier, token, name));
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
    void TC_CAT_12_xoa_trong_race_voi_tao_product_thuoc_category_do() throws Exception {
        String token = adminToken();
        Map<String, Object> category = mustCreateCategory(token, "Race Cat " + uniqueSuffix(), "desc");
        long catId = idOf(category);
        CyclicBarrier barrier = new CyclicBarrier(2);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> deleteResult = pool.submit(() -> {
                barrier.await();
                return deleteWithBearer("/api/v1/admin/categories/" + catId, token).getStatusCode().value();
            });
            Future<Integer> createResult = pool.submit(() -> {
                barrier.await();
                return postWithBearer("/api/v1/admin/products",
                        Map.of("sku", "RACE-" + uniqueSuffix(), "name", "Race Product",
                                "price", new java.math.BigDecimal("50000"),
                                "categoryId", catId, "initialStock", 1), token
                ).getStatusCode().value();
            });
            int deleteStatus = deleteResult.get();
            int createStatus = createResult.get();

            // Không bên nào 500; một bên thắng, bên kia 409 (FK race) hoặc 404 (delete thắng hoàn toàn trước)
            assertThat(deleteStatus).isIn(200, 409);
            assertThat(createStatus).isIn(201, 404, 409);
            assertThat(deleteStatus).isNotEqualTo(500);
            assertThat(createStatus).isNotEqualTo(500);
            assertThat(deleteStatus == 200 || createStatus == 201).isTrue();
        } finally {
            pool.shutdownNow();
        }
    }

    private final class CallableStatusCallable implements java.util.concurrent.Callable<Integer> {
        private final CyclicBarrier barrier;
        private final String token;
        private final String name;

        CallableStatusCallable(CyclicBarrier barrier, String token, String name) {
            this.barrier = barrier;
            this.token = token;
            this.name = name;
        }

        @Override
        public Integer call() throws Exception {
            barrier.await();
            return postWithBearer("/api/v1/admin/categories",
                    Map.of("name", name, "description", "desc"), token).getStatusCode().value();
        }
    }
}
