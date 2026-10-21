package com.app.backend.product;

import com.app.backend.common.CatalogIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TC-AUTHZ-01: mọi endpoint admin Phase 2 — không token → 401; token USER → 403.
 */
class ProductAuthzIntegrationTest extends CatalogIntegrationTestBase {

    static Stream<Arguments> adminEndpoints() {
        return Stream.of(
                Arguments.of("GET", "/api/v1/admin/categories", null),
                Arguments.of("POST", "/api/v1/admin/categories", Map.of("name", "authz", "description", "x")),
                Arguments.of("PUT", "/api/v1/admin/categories/1", Map.of("name", "authz", "description", "x")),
                Arguments.of("DELETE", "/api/v1/admin/categories/1", null),
                Arguments.of("GET", "/api/v1/admin/products", null),
                Arguments.of("GET", "/api/v1/admin/products/1", null),
                Arguments.of("POST", "/api/v1/admin/products", Map.of("sku", "AUTHZ-1", "name", "x",
                        "price", new BigDecimal("1000"), "categoryId", 1, "initialStock", 1)),
                Arguments.of("PUT", "/api/v1/admin/products/1", Map.of("name", "x", "description", "d",
                        "price", new BigDecimal("1000"), "categoryId", 1, "status", "ACTIVE", "version", 0)),
                Arguments.of("PATCH", "/api/v1/admin/products/1/status", Map.of("status", "INACTIVE")));
    }

    @ParameterizedTest(name = "{0} {1} không token → 401")
    @MethodSource("adminEndpoints")
    void TC_AUTHZ_01_khong_token_401(String method, String path, Object body) {
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(path, HttpMethod.valueOf(method),
                new HttpEntity<>(body, jsonHeaders()), mapType());
        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @ParameterizedTest(name = "{0} {1} token USER → 403")
    @MethodSource("adminEndpoints")
    void TC_AUTHZ_01_token_user_403(String method, String path, Object body) {
        String email = uniqueEmail("authz");
        mustRegister(email, "MatKhau123");
        String userToken = accessTokenOf(mustLogin(email, "MatKhau123"));

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(path, HttpMethod.valueOf(method),
                new HttpEntity<>(body, bearerHeaders(userToken)), mapType());
        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(errorCodeOf(response)).isEqualTo("ACCESS_DENIED");
    }

    @Test
    void TC_AUTHZ_01_public_get_khong_can_token() {
        assertThat(getPublic("/api/v1/products").getStatusCode().value()).isEqualTo(200);
        assertThat(getPublic("/api/v1/categories").getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void TC_AUTHZ_01_method_ghi_public_khong_duoc_permitAll() {
        // POST/PUT/DELETE/PATCH tới /products, /categories (không phải /admin) phải authenticated
        ResponseEntity<Map<String, Object>> post = restTemplate.exchange("/api/v1/categories",
                HttpMethod.POST, new HttpEntity<>(Map.of("name", "x"), jsonHeaders()), mapType());
        assertThat(post.getStatusCode().value()).isEqualTo(401);

        ResponseEntity<Map<String, Object>> patch = restTemplate.exchange("/api/v1/products/1/status",
                HttpMethod.PATCH, new HttpEntity<>(Map.of("status", "ACTIVE"), jsonHeaders()), mapType());
        assertThat(patch.getStatusCode().value()).isEqualTo(401);
    }
}
