package com.app.backend.common.security;

import com.app.backend.user.Role;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "unit-test-jwt-secret-32-bytes-minimum-abc";

    @Test
    void TC_AUTH_UT_01_issue_token_claim_day_du() {
        JwtService jwtService = new JwtService(SECRET);

        JwtService.IssuedToken issued = jwtService.issueAccessToken(42L, Role.USER, "sid-1");
        Jwt jwt = jwtService.decode(issued.token());
        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.getClaimAsString("jti")).isEqualTo(issued.jti());
        assertThat(jwt.getClaimAsString("sid")).isEqualTo("sid-1");
        assertThat(jwt.getClaimAsString("role")).isEqualTo("USER");
        assertThat(jwt.getClaimAsString("type")).isEqualTo("access");
        long accessTtl = jwt.getExpiresAt().getEpochSecond() - jwt.getIssuedAt().getEpochSecond();
        assertThat(accessTtl).isEqualTo(15 * 60L);
        long now = Instant.now().getEpochSecond();
        assertThat(jwt.getExpiresAt().getEpochSecond()).isBetween(now + 14 * 60, now + 15 * 60 + 5);

        JwtService.IssuedToken refreshIssued = jwtService.issueRefreshToken(42L, "sid-1");
        Jwt refresh = jwtService.decode(refreshIssued.token());
        assertThat(refresh.getClaimAsString("type")).isEqualTo("refresh");
        assertThat(refresh.getClaimAsString("role")).isNull();
        long refreshTtl = refresh.getExpiresAt().getEpochSecond() - refresh.getIssuedAt().getEpochSecond();
        assertThat(refreshTtl).isEqualTo(7 * 24 * 60 * 60L);
    }

    @Test
    void TC_AUTH_UT_03_secret_ngan_that_bai_khi_khoi_dong() {
        // ApplicationContext fail-fast: JwtService là bean — constructor ném là context không khởi động được
        assertThatThrownBy(() -> new JwtService("too-short-secret"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("32");
    }
}
