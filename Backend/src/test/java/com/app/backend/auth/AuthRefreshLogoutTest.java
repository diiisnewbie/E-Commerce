package com.app.backend.auth;

import com.app.backend.auth.dto.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class AuthRefreshLogoutTest extends AuthIntegrationTestBase {

    private static final String REFRESH = "/api/v1/auth/refresh";
    private static final String LOGOUT = "/api/v1/auth/logout";
    private static final String ME = "/api/v1/auth/me";

    @Test
    void TC_AUTH_11_refresh_thanh_cong() {
        String email = uniqueEmail("Refresh1");
        mustRegister(email, "MatKhau123");
        Map<String, Object> first = mustLogin(email, "MatKhau123");

        ResponseEntity<Map<String, Object>> response = postJson(REFRESH,
                Map.of("refreshToken", refreshTokenOf(first)));
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        Map<String, Object> second = dataOf(response);

        assertThat(accessTokenOf(second)).isNotEqualTo(accessTokenOf(first));
        assertThat(refreshTokenOf(second)).isNotEqualTo(refreshTokenOf(first));
        assertThat(userIdOf(second)).isEqualTo(userIdOf(first));

        // Dùng lại token cũ để refresh → 401
        ResponseEntity<Map<String, Object>> reuse = postJson(REFRESH,
                Map.of("refreshToken", refreshTokenOf(first)));
        assertThat(reuse.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void TC_AUTH_12_dung_lai_refresh_da_rotate() {
        String email = uniqueEmail("Reuse");
        mustRegister(email, "MatKhau123");
        Map<String, Object> first = mustLogin(email, "MatKhau123");
        Map<String, Object> rotated = dataOf(postJson(REFRESH,
                Map.of("refreshToken", refreshTokenOf(first))));

        ResponseEntity<Map<String, Object>> replay = postJson(REFRESH,
                Map.of("refreshToken", refreshTokenOf(first)));
        assertThat(replay.getStatusCode().value()).isEqualTo(401);
        assertThat(errorCodeOf(replay)).isEqualTo("TOKEN_REVOKED");

        // Access token vừa cấp trước đó cũng bị từ chối (phiên đã thu hồi)
        ResponseEntity<Map<String, Object>> newAccess = getWithBearer(ME, accessTokenOf(rotated));
        assertThat(newAccess.getStatusCode().value()).isEqualTo(401);
        assertThat(errorCodeOf(newAccess)).isEqualTo("TOKEN_REVOKED");
    }

    @Test
    void TC_AUTH_13_logout_roi_me_bi_tu_choi() {
        // Cặp test từ chối của /logout: không kèm token → 401
        ResponseEntity<Map<String, Object>> withoutToken = restTemplate.exchange(
                LOGOUT, HttpMethod.POST, new HttpEntity<>(new HttpHeaders()), mapType());
        assertThat(withoutToken.getStatusCode().value()).isEqualTo(401);
        assertThat(errorCodeOf(withoutToken)).isEqualTo("TOKEN_INVALID");

        String email = uniqueEmail("Logout");
        mustRegister(email, "MatKhau123");
        Map<String, Object> tokens = mustLogin(email, "MatKhau123");
        String access = accessTokenOf(tokens);

        ResponseEntity<Map<String, Object>> logout = postWithBearer(LOGOUT, null, access);
        assertThat(logout.getStatusCode().value()).isEqualTo(200);

        ResponseEntity<Map<String, Object>> me = getWithBearer(ME, access);
        assertThat(me.getStatusCode().value()).isEqualTo(401);
        assertThat(errorCodeOf(me)).isEqualTo("TOKEN_REVOKED");
    }

    @Test
    void TC_AUTH_17_access_token_gui_vao_refresh() {
        String email = uniqueEmail("AccessInRefresh");
        mustRegister(email, "MatKhau123");
        Map<String, Object> tokens = mustLogin(email, "MatKhau123");

        ResponseEntity<Map<String, Object>> response = postJson(REFRESH,
                Map.of("refreshToken", accessTokenOf(tokens)));
        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(errorCodeOf(response)).isEqualTo("TOKEN_INVALID");

        // Phiên không bị thu hồi — access token vẫn gọi /me thành công
        ResponseEntity<Map<String, Object>> me = getWithBearer(ME, accessTokenOf(tokens));
        assertThat(me.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void TC_AUTH_19_bearer_het_han_kem_refresh_va_login() {
        String email = uniqueEmail("ExpiredBearer");
        mustRegister(email, "MatKhau123");
        Map<String, Object> tokens = mustLogin(email, "MatKhau123");
        String expired = signAccessToken(userIdOf(tokens), "USER", sidOf(accessTokenOf(tokens)),
                Instant.now().minusSeconds(10));

        // Filter không chặn endpoint permitAll — request đi tới controller
        ResponseEntity<Map<String, Object>> refresh = postWithBearer(REFRESH,
                Map.of("refreshToken", refreshTokenOf(tokens)), expired);
        assertThat(refresh.getStatusCode().value()).isEqualTo(200);

        ResponseEntity<Map<String, Object>> login = postWithBearer("/api/v1/auth/login",
                new LoginRequest(email, "MatKhau123"), expired);
        assertThat(login.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void TC_AUTH_20_hai_refresh_song_song_cung_token() throws Exception {
        String email = uniqueEmail("Parallel");
        mustRegister(email, "MatKhau123");
        Map<String, Object> tokens = mustLogin(email, "MatKhau123");
        String oldRefresh = refreshTokenOf(tokens);
        CyclicBarrier barrier = new CyclicBarrier(2);
        Callable<ResponseEntity<Map<String, Object>>> request = () -> {
            barrier.await(10, TimeUnit.SECONDS);
            return postJson(REFRESH, Map.of("refreshToken", oldRefresh));
        };

        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<ResponseEntity<Map<String, Object>>> results;
        try {
            Future<ResponseEntity<Map<String, Object>>> first = pool.submit(request);
            Future<ResponseEntity<Map<String, Object>>> second = pool.submit(request);
            results = List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }

        // Tối đa một 200 (GETDEL nguyên tử) — không có case 500
        for (ResponseEntity<Map<String, Object>> result : results) {
            assertThat(result.getStatusCode().value()).isIn(200, 401);
        }
        List<ResponseEntity<Map<String, Object>>> winners = results.stream()
                .filter(result -> result.getStatusCode().value() == 200)
                .toList();
        assertThat(winners).hasSizeLessThanOrEqualTo(1);

        // Sau khi cả hai request kết thúc, phiên bị thu hồi
        ResponseEntity<Map<String, Object>> oldAccess = getWithBearer(ME, accessTokenOf(tokens));
        assertThat(oldAccess.getStatusCode().value()).isEqualTo(401);
        assertThat(errorCodeOf(oldAccess)).isEqualTo("TOKEN_REVOKED");

        ResponseEntity<Map<String, Object>> oldRefreshReuse = postJson(REFRESH,
                Map.of("refreshToken", oldRefresh));
        assertThat(oldRefreshReuse.getStatusCode().value()).isEqualTo(401);

        if (!winners.isEmpty()) {
            Map<String, Object> newTokens = dataOf(winners.getFirst());
            ResponseEntity<Map<String, Object>> newAccess = getWithBearer(ME, accessTokenOf(newTokens));
            assertThat(newAccess.getStatusCode().value()).isEqualTo(401);
            assertThat(errorCodeOf(newAccess)).isEqualTo("TOKEN_REVOKED");

            ResponseEntity<Map<String, Object>> newRefresh = postJson(REFRESH,
                    Map.of("refreshToken", refreshTokenOf(newTokens)));
            assertThat(newRefresh.getStatusCode().value()).isEqualTo(401);
            assertThat(errorCodeOf(newRefresh)).isEqualTo("TOKEN_REVOKED");
        }
    }

    @Test
    void TC_AUTH_21_logout_roi_refresh_cu() {
        String email = uniqueEmail("LogoutThenRefresh");
        mustRegister(email, "MatKhau123");
        Map<String, Object> tokens = mustLogin(email, "MatKhau123");
        String refresh = refreshTokenOf(tokens);

        ResponseEntity<Map<String, Object>> logout = postWithBearer(LOGOUT, null, accessTokenOf(tokens));
        assertThat(logout.getStatusCode().value()).isEqualTo(200);

        // Key đã bị xóa nên GETDEL trả null → TOKEN_REVOKED
        ResponseEntity<Map<String, Object>> response = postJson(REFRESH,
                Map.of("refreshToken", refresh));
        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(errorCodeOf(response)).isEqualTo("TOKEN_REVOKED");
    }
}
