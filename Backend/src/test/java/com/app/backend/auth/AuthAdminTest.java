package com.app.backend.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuthAdminTest extends AuthIntegrationTestBase {

    private static final String ADMIN_STATS = "/api/v1/admin/stats";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void TC_AUTH_14_user_role_khong_vao_duoc_admin() {
        String email = uniqueEmail("NotAdmin");
        mustRegister(email, "MatKhau123");
        Map<String, Object> tokens = mustLogin(email, "MatKhau123");

        ResponseEntity<Map<String, Object>> forbidden = getWithBearer(ADMIN_STATS, accessTokenOf(tokens));
        assertThat(forbidden.getStatusCode().value()).isEqualTo(403);
        assertThat(errorCodeOf(forbidden)).isEqualTo("ACCESS_DENIED");

        ResponseEntity<Map<String, Object>> unauthorized = restTemplate.exchange(
                ADMIN_STATS, HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), mapType());
        assertThat(unauthorized.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void TC_AUTH_15_admin_seed_login_va_hash_nguyen_ven() {
        Map<String, Object> admin = mustLogin("admin@ecommerce.dev", "Admin@123");
        @SuppressWarnings("unchecked")
        Map<String, Object> adminUser = (Map<String, Object>) admin.get("user");
        assertThat(adminUser.get("role")).isEqualTo("ADMIN");

        ResponseEntity<Map<String, Object>> stats = getWithBearer(ADMIN_STATS, accessTokenOf(admin));
        assertThat(stats.getStatusCode().value()).isEqualTo(200);
        assertThat(((Number) dataOf(stats).get("totalUsers")).longValue()).isGreaterThanOrEqualTo(1);

        String hash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM users WHERE email = ?", String.class, "admin@ecommerce.dev");
        // Hash còn nguyên ký tự $ (Flyway placeholder không phá hash — B8)
        assertThat(hash).isNotNull();
        assertThat(hash).startsWith("$2a$10$");
        assertThat(hash).matches("\\$2[a-z]\\$\\d\\d\\$.+");
    }
}
