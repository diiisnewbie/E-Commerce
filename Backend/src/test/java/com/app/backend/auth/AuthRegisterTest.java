package com.app.backend.auth;

import com.app.backend.user.Role;
import com.app.backend.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class AuthRegisterTest extends AuthIntegrationTestBase {

    private static final String REGISTER = "/api/v1/auth/register";

    @Test
    void TC_AUTH_01_register_hop_le() {
        String email = uniqueEmail("NguyenVanA");
        ResponseEntity<Map<String, Object>> response = postJson(REGISTER, Map.of(
                "email", email,
                "password", "MatKhau123",
                "fullName", "Nguyen Van A"));

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        String normalized = email.toLowerCase(Locale.ROOT);
        assertThat(dataOf(response).get("email")).isEqualTo(normalized);
        assertThat(dataOf(response).get("role")).isEqualTo("USER");

        User user = userRepository.findByEmail(normalized).orElseThrow();
        assertThat(user.getPasswordHash()).startsWith("$2a$");
        assertThat(user.getPasswordHash()).isNotEqualTo("MatKhau123");
        assertThat(user.getFullName()).isEqualTo("Nguyen Van A");
        assertThat(user.getRole()).isEqualTo(Role.USER);
    }

    @Test
    void TC_AUTH_02_register_email_trung_khac_kieu_chu() {
        String email = uniqueEmail("Dup");
        assertThat(postJson(REGISTER, Map.of(
                "email", email, "password", "MatKhau123", "fullName", "Dup User"))
                .getStatusCode().value()).isEqualTo(201);

        int atIndex = email.indexOf('@');
        String differentCase = email.substring(0, atIndex).toUpperCase(Locale.ROOT) + email.substring(atIndex);
        ResponseEntity<Map<String, Object>> duplicate = postJson(REGISTER, Map.of(
                "email", differentCase, "password", "MatKhau123", "fullName", "Dup User"));

        assertThat(duplicate.getStatusCode().value()).isEqualTo(409);
        assertThat(errorCodeOf(duplicate)).isEqualTo("EMAIL_ALREADY_EXISTS");
    }

    @Test
    void TC_AUTH_03_register_input_khong_hop_le() {
        ResponseEntity<Map<String, Object>> badEmail = postJson(REGISTER, Map.of(
                "email", "not-an-email", "password", "MatKhau123", "fullName", "A B"));
        ResponseEntity<Map<String, Object>> tooShort = postJson(REGISTER, Map.of(
                "email", uniqueEmail("Short"), "password", "Ab1!", "fullName", "A B"));
        // 30 ký tự tiếng Việt có dấu × 3 byte = 90 byte UTF-8 (vượt giới hạn 72 byte của BCrypt)
        ResponseEntity<Map<String, Object>> tooManyBytes = postJson(REGISTER, Map.of(
                "email", uniqueEmail("ManyBytes"), "password", "ậ".repeat(30), "fullName", "A B"));

        for (ResponseEntity<Map<String, Object>> response : List.of(badEmail, tooShort, tooManyBytes)) {
            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(errorCodeOf(response)).isEqualTo("VALIDATION_ERROR");
        }
    }

    @Test
    void TC_AUTH_22_role_trong_body_bi_bo_qua() {
        String email = uniqueEmail("Role");
        Map<String, Object> body = new HashMap<>();
        body.put("email", email);
        body.put("password", "MatKhau123");
        body.put("fullName", "Role User");
        body.put("role", "ADMIN");

        ResponseEntity<Map<String, Object>> response = postJson(REGISTER, body);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(dataOf(response).get("role")).isEqualTo("USER");
        User user = userRepository.findByEmail(email.toLowerCase(Locale.ROOT)).orElseThrow();
        assertThat(user.getRole()).isEqualTo(Role.USER);
    }

    @Test
    void TC_AUTH_27_hai_register_song_song_cung_email() throws Exception {
        String email = uniqueEmail("Race");
        Map<String, Object> body = Map.of(
                "email", email, "password", "MatKhau123", "fullName", "Race User");
        CyclicBarrier barrier = new CyclicBarrier(2);
        Callable<Integer> request = () -> {
            barrier.await(10, TimeUnit.SECONDS);
            return postJson(REGISTER, body).getStatusCode().value();
        };

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> first = pool.submit(request);
            Future<Integer> second = pool.submit(request);
            int status1 = first.get(30, TimeUnit.SECONDS);
            int status2 = second.get(30, TimeUnit.SECONDS);

            // Một bên 201, một bên 409 — không bên nào nhận 500
            assertThat(List.of(status1, status2)).containsExactlyInAnyOrder(201, 409);
        } finally {
            pool.shutdownNow();
        }
    }
}
