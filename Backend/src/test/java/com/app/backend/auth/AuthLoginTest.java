package com.app.backend.auth;

import com.app.backend.auth.dto.LoginRequest;
import com.app.backend.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuthLoginTest extends AuthIntegrationTestBase {

    private static final String REGISTER = "/api/v1/auth/register";
    private static final String LOGIN = "/api/v1/auth/login";

    @Test
    void TC_AUTH_04_login_hop_le() {
        String email = uniqueEmail("Login");
        mustRegister(email, "MatKhau123");

        Map<String, Object> tokens = mustLogin(email, "MatKhau123");

        assertThat(tokens.get("tokenType")).isEqualTo("Bearer");
        assertThat(((Number) tokens.get("expiresInSeconds")).longValue()).isEqualTo(900);
        assertThat(accessTokenOf(tokens)).isNotBlank();
        assertThat(refreshTokenOf(tokens)).isNotBlank();

        Jwt refresh = decode(refreshTokenOf(tokens));
        assertThat(refresh.getClaimAsString("type")).isEqualTo("refresh");
        assertThat(refresh.getSubject()).isEqualTo(String.valueOf(userIdOf(tokens)));
        assertThat(refresh.getClaimAsString("sid")).isNotBlank();
        long ttl = refresh.getExpiresAt().getEpochSecond() - refresh.getIssuedAt().getEpochSecond();
        assertThat(ttl).isEqualTo(7 * 24 * 60 * 60L);
    }

    @Test
    void TC_AUTH_05_login_sai_mat_khau() {
        String email = uniqueEmail("WrongPw");
        mustRegister(email, "MatKhau123");

        ResponseEntity<Map<String, Object>> response = postJson(LOGIN,
                new LoginRequest(email, "SaiMatKhau123"));

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(errorCodeOf(response)).isEqualTo("INVALID_CREDENTIALS");
    }

    @Test
    void TC_AUTH_06_login_email_khong_ton_tai_cung_message() {
        ResponseEntity<Map<String, Object>> unknown = postJson(LOGIN,
                new LoginRequest(uniqueEmail("Unknown"), "MatKhau123"));

        String email = uniqueEmail("Known");
        mustRegister(email, "MatKhau123");
        ResponseEntity<Map<String, Object>> wrongPassword = postJson(LOGIN,
                new LoginRequest(email, "SaiMatKhau123"));

        assertThat(unknown.getStatusCode().value()).isEqualTo(401);
        assertThat(wrongPassword.getStatusCode().value()).isEqualTo(401);
        assertThat(errorCodeOf(unknown)).isEqualTo("INVALID_CREDENTIALS");
        assertThat(errorCodeOf(wrongPassword)).isEqualTo("INVALID_CREDENTIALS");
        // Chống dò tài khoản: email không tồn tại và sai mật khẩu trả cùng message
        assertThat(messageOf(unknown)).isEqualTo(messageOf(wrongPassword));
    }

    @Test
    void TC_AUTH_23_login_user_bi_khoa() {
        String email = uniqueEmail("Locked");
        mustRegister(email, "MatKhau123");
        User user = userRepository.findByEmail(email.toLowerCase(Locale.ROOT)).orElseThrow();
        user.setActive(false);
        userRepository.save(user);

        ResponseEntity<Map<String, Object>> response = postJson(LOGIN,
                new LoginRequest(email, "MatKhau123"));

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(errorCodeOf(response)).isEqualTo("INVALID_CREDENTIALS");
    }

    @Test
    void TC_AUTH_26_gioi_han_mat_khau_theo_byte() {
        // 24 ký tự tiếng Việt × 3 byte = đúng 72 byte UTF-8 (ranh giới trên)
        String boundaryPassword = "ậ".repeat(24);
        String email = uniqueEmail("Byte");
        assertThat(mustRegister(email, boundaryPassword).get("email")).isEqualTo(email.toLowerCase(Locale.ROOT));
        assertThat(mustLogin(email, boundaryPassword).get("tokenType")).isEqualTo("Bearer");

        // ASCII 73 byte → vượt giới hạn bcrypt
        String overBytes = "a".repeat(73);
        ResponseEntity<Map<String, Object>> registerOver = postJson(REGISTER, Map.of(
                "email", uniqueEmail("Over"), "password", overBytes, "fullName", "A B"));
        ResponseEntity<Map<String, Object>> loginOver = postJson(LOGIN,
                new LoginRequest(email, overBytes));

        assertThat(registerOver.getStatusCode().value()).isEqualTo(400);
        assertThat(errorCodeOf(registerOver)).isEqualTo("VALIDATION_ERROR");
        assertThat(loginOver.getStatusCode().value()).isEqualTo(400);
        assertThat(errorCodeOf(loginOver)).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void TC_AUTH_28_json_hong_tra_400() {
        ResponseEntity<Map<String, Object>> response = postRawJson(LOGIN,
                "{\"email\": \"a@b.c\", \"pass");

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(errorCodeOf(response)).isEqualTo("VALIDATION_ERROR");
    }
}
