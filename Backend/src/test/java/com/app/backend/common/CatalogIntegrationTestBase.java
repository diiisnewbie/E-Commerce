package com.app.backend.common;

import com.app.backend.auth.AuthIntegrationTestBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Support dùng chung cho integration test Phase 2 (category/product):
 * helper admin token, tạo category/product, HTTP verb mở rộng.
 */
public abstract class CatalogIntegrationTestBase extends AuthIntegrationTestBase {

    private static final String ADMIN_EMAIL = "admin@ecommerce.dev";
    private static final String ADMIN_PASSWORD = "Admin@123";

    private static volatile String cachedAdminToken;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    protected String adminToken() {
        String token = cachedAdminToken;
        if (token == null) {
            synchronized (CatalogIntegrationTestBase.class) {
                if (cachedAdminToken == null) {
                    cachedAdminToken = accessTokenOf(mustLogin(ADMIN_EMAIL, ADMIN_PASSWORD));
                }
                token = cachedAdminToken;
            }
        }
        return token;
    }

    protected String uniqueSuffix() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase(java.util.Locale.ROOT);
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> mustCreateCategory(String token, String name, String description) {
        ResponseEntity<Map<String, Object>> response = postWithBearer("/api/v1/admin/categories",
                Map.of("name", name, "description", description), token);
        if (response.getStatusCode().value() != 201) {
            throw new IllegalStateException("Create category failed: " + response.getBody());
        }
        return dataOf(response);
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> mustCreateProduct(String token, String sku, String name,
                                                    BigDecimal price, Long categoryId, long initialStock) {
        ResponseEntity<Map<String, Object>> response = postWithBearer("/api/v1/admin/products",
                Map.of("sku", sku, "name", name, "price", price,
                        "categoryId", categoryId, "initialStock", initialStock),
                token);
        if (response.getStatusCode().value() != 201) {
            throw new IllegalStateException("Create product failed: " + response.getBody());
        }
        return dataOf(response);
    }

    protected long idOf(Map<String, Object> data) {
        return ((Number) data.get("id")).longValue();
    }

    protected ResponseEntity<Map<String, Object>> putWithBearer(String path, Object body, String token) {
        return restTemplate.exchange(path, HttpMethod.PUT, new HttpEntity<>(body, bearerHeaders(token)), mapType());
    }

    protected ResponseEntity<Map<String, Object>> patchWithBearer(String path, Object body, String token) {
        return restTemplate.exchange(path, HttpMethod.PATCH, new HttpEntity<>(body, bearerHeaders(token)), mapType());
    }

    protected ResponseEntity<Map<String, Object>> deleteWithBearer(String path, String token) {
        return restTemplate.exchange(path, HttpMethod.DELETE, new HttpEntity<>(bearerHeaders(token)), mapType());
    }

    protected ResponseEntity<Map<String, Object>> getPublic(String path) {
        return restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(new org.springframework.http.HttpHeaders()), mapType());
    }

    @SuppressWarnings("unchecked")
    protected static List<Map<String, Object>> contentOf(ResponseEntity<Map<String, Object>> response) {
        Map<String, Object> data = dataOf(response);
        return (List<Map<String, Object>>) data.get("content");
    }

    @SuppressWarnings("unchecked")
    protected static Map<String, Object> fieldsOf(ResponseEntity<Map<String, Object>> response) {
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        return data == null ? Map.of() : (Map<String, Object>) data.getOrDefault("fields", Map.of());
    }
}
