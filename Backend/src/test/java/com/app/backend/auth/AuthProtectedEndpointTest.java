package com.app.backend.auth;

import com.app.backend.auth.dto.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuthProtectedEndpointTest extends AuthIntegrationTestBase {

    private static final String REGISTER = "/api/v1/auth/register";
    private static final String LOGIN = "/api/v1/auth/login";
    private static final String ME = "/api/v1/auth/me";
    private static final String REFRESH = "/api/v1/auth/refresh";

    @Test
    void TC_AUTH_07_me_voi_access_token_hop_le() {
        String email = uniqueEmail("MeOk");
        mustRegister(email, "MatKhau123");
        Map<String, Object> tokens = mustLogin(email, "MatKhau123");

        ResponseEntity<Map<String, Object>> response = getWithBearer(ME, accessTokenOf(tokens));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        // Email trong DB/response luôn là bản lowercase
        assertThat(dataOf(response).get("email")).isEqualTo(email.toLowerCase(Locale.ROOT));
        assertThat(((Number) dataOf(response).get("id")).longValue()).isEqualTo(userIdOf(tokens));
    }

    @Test
    void TC_AUTH_08_me_khong_kem_token() {
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                ME, HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), mapType());

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(errorCodeOf(response)).isEqualTo("TOKEN_INVALID");
        assertThat(messageOf(response)).isEqualTo("Authentication required");
    }

    @Test
    void TC_AUTH_09_access_token_het_han_10_giay_truoc() {
        String email = uniqueEmail("Expired");
        mustRegister(email, "MatKhau123");
        Map<String, Object> tokens = mustLogin(email, "MatKhau123");
        String expired = signAccessToken(userIdOf(tokens), "USER", sidOf(accessTokenOf(tokens)),
                Instant.now().minusSeconds(10));

        ResponseEntity<Map<String, Object>> response = getWithBearer(ME, expired);

        // Nằm trong clock skew 60s của JwtTimestampValidator mặc định — chỉ đúng khi đã bỏ timestamp validator (D-1-15)
        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(errorCodeOf(response)).isEqualTo("TOKEN_EXPIRED");
    }

    @Test
    void TC_AUTH_10_token_sua_chu_ky_hoac_thieu_prefix_bearer() {
        String email = uniqueEmail("Tamper");
        mustRegister(email, "MatKhau123");
        Map<String, Object> tokens = mustLogin(email, "MatKhau123");
        String access = accessTokenOf(tokens);

        // (a) Sửa chữ ký (đổi 1 ký tự cuối)
        char last = access.charAt(access.length() - 1);
        String tampered = access.substring(0, access.length() - 1) + (last == 'A' ? 'B' : 'A');
        ResponseEntity<Map<String, Object>> forged = getWithBearer(ME, tampered);
        assertThat(forged.getStatusCode().value()).isEqualTo(401);
        assertThat(errorCodeOf(forged)).isEqualTo("TOKEN_INVALID");

        // (b) Gửi token không kèm prefix "Bearer "
        HttpHeaders rawHeader = new HttpHeaders();
        rawHeader.set(HttpHeaders.AUTHORIZATION, access);
        ResponseEntity<Map<String, Object>> raw = restTemplate.exchange(
                ME, HttpMethod.GET, new HttpEntity<>(rawHeader), mapType());
        assertThat(raw.getStatusCode().value()).isEqualTo(401);
        assertThat(errorCodeOf(raw)).isEqualTo("TOKEN_INVALID");
    }

    @Test
    void TC_AUTH_16_sid_bi_blacklist() {
        String email = uniqueEmail("Sid1");
        mustRegister(email, "MatKhau123");
        Map<String, Object> firstLogin = mustLogin(email, "MatKhau123");
        Map<String, Object> secondLogin = mustLogin(email, "MatKhau123");
        String revokedSid = sidOf(accessTokenOf(secondLogin));

        // Mô phỏng logout phiên khác cùng user
        refreshTokenStore.markRevoked(revokedSid, Duration.ofMinutes(15));

        ResponseEntity<Map<String, Object>> revokedSession = getWithBearer(ME, accessTokenOf(secondLogin));
        assertThat(revokedSession.getStatusCode().value()).isEqualTo(401);
        assertThat(errorCodeOf(revokedSession)).isEqualTo("TOKEN_REVOKED");

        // Phiên khác vẫn hoạt động
        ResponseEntity<Map<String, Object>> otherSession = getWithBearer(ME, accessTokenOf(firstLogin));
        assertThat(otherSession.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void TC_AUTH_18_refresh_token_dung_lam_bearer() {
        String email = uniqueEmail("RefreshAsBearer");
        mustRegister(email, "MatKhau123");
        Map<String, Object> tokens = mustLogin(email, "MatKhau123");
        String refresh = refreshTokenOf(tokens);

        ResponseEntity<Map<String, Object>> asBearer = getWithBearer(ME, refresh);
        assertThat(asBearer.getStatusCode().value()).isEqualTo(401);
        assertThat(errorCodeOf(asBearer)).isEqualTo("TOKEN_INVALID");

        // Phiên không bị thu hồi — refresh vẫn dùng để refresh thành công
        ResponseEntity<Map<String, Object>> stillWorks = postJson(REFRESH, Map.of("refreshToken", refresh));
        assertThat(stillWorks.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void TC_AUTH_24_alg_none_va_hs512_bi_tu_choi() {
        String email = uniqueEmail("AlgAttack");
        mustRegister(email, "MatKhau123");
        Map<String, Object> tokens = mustLogin(email, "MatKhau123");
        long userId = userIdOf(tokens);
        String sid = sidOf(accessTokenOf(tokens));

        ResponseEntity<Map<String, Object>> none = getWithBearer(ME,
                unsignedToken(userId, sid, Instant.now().plusSeconds(300)));
        assertThat(none.getStatusCode().value()).isEqualTo(401);
        assertThat(errorCodeOf(none)).isEqualTo("TOKEN_INVALID");

        ResponseEntity<Map<String, Object>> hs512 = getWithBearer(ME,
                hs512Token(userId, sid, Instant.now().plusSeconds(300)));
        assertThat(hs512.getStatusCode().value()).isEqualTo(401);
        assertThat(errorCodeOf(hs512)).isEqualTo("TOKEN_INVALID");
    }

    @Test
    void TC_AUTH_25_response_khong_chua_password() {
        String email = uniqueEmail("NoPwField");
        ResponseEntity<String> register = restTemplate.exchange(REGISTER, HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "email", email, "password", "MatKhau123", "fullName", "No Pw"),
                        jsonHeaders()),
                String.class);
        ResponseEntity<String> login = restTemplate.exchange(LOGIN, HttpMethod.POST,
                new HttpEntity<>(new LoginRequest(email, "MatKhau123"), jsonHeaders()),
                String.class);
        Map<String, Object> tokens = mustLogin(email, "MatKhau123");
        ResponseEntity<String> me = restTemplate.exchange(ME, HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(accessTokenOf(tokens))), String.class);

        for (ResponseEntity<String> response : List.of(register, login, me)) {
            assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).doesNotContain("passwordHash");
            assertThat(response.getBody()).doesNotContain("\"password\"");
        }
    }
}
