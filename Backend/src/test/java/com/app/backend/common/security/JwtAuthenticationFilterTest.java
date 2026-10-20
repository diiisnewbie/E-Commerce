package com.app.backend.common.security;

import com.app.backend.auth.InMemoryRefreshTokenStore;
import com.app.backend.user.Role;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationFilterTest {

    private static final String SECRET = "unit-test-jwt-secret-32-bytes-minimum-abc";

    private final JwtService jwtService = new JwtService(SECRET);
    private final InMemoryRefreshTokenStore refreshTokenStore = new InMemoryRefreshTokenStore();
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, refreshTokenStore);

    record FilterResult(Object errorAttribute, boolean authenticated) {
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void TC_AUTH_UT_02_anh_xa_token_sai_het_han_refresh() {
        // (a) Token ký bằng secret khác → JwtException → TOKEN_INVALID, không authenticate
        JwtService otherService = new JwtService("another-secret-32-bytes-minimum-abcdef");
        String forged = otherService.issueAccessToken(1L, Role.USER, "sid-a").token();
        FilterResult invalid = run("Bearer " + forged);
        assertThat(invalid.errorAttribute()).isEqualTo("TOKEN_INVALID");
        assertThat(invalid.authenticated()).isFalse();

        // (b) Chữ ký đúng nhưng exp quá khứ 10s — decoder KHÔNG có timestamp validator vẫn decode được,
        // filter tự kiểm exp → TOKEN_EXPIRED (D-1-15)
        String expired = signExpiredAccessToken();
        Jwt decoded = jwtService.decode(expired);
        assertThat(decoded.getExpiresAt()).isBefore(Instant.now());
        FilterResult expiredResult = run("Bearer " + expired);
        assertThat(expiredResult.errorAttribute()).isEqualTo("TOKEN_EXPIRED");
        assertThat(expiredResult.authenticated()).isFalse();

        // (c) type=refresh gửi vào API bảo vệ → TOKEN_INVALID
        String refresh = jwtService.issueRefreshToken(1L, "sid-a").token();
        FilterResult refreshResult = run("Bearer " + refresh);
        assertThat(refreshResult.errorAttribute()).isEqualTo("TOKEN_INVALID");
        assertThat(refreshResult.authenticated()).isFalse();
    }

    private FilterResult run(String authorizationValue) {
        SecurityContextHolder.clearContext();
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (authorizationValue != null) {
            request.addHeader("Authorization", authorizationValue);
        }
        try {
            filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        } catch (Exception e) {
            throw new IllegalStateException("Filter không nên ném exception", e);
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return new FilterResult(request.getAttribute(JwtAuthenticationFilter.ERROR_ATTRIBUTE),
                authentication != null && authentication.isAuthenticated());
    }

    private String signExpiredAccessToken() {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject("1")
                    .jwtID(UUID.randomUUID().toString())
                    .claim("sid", "sid-a")
                    .claim("role", "USER")
                    .claim("type", "access")
                    .issueTime(Date.from(Instant.now().minusSeconds(910)))
                    .expirationTime(Date.from(Instant.now().minusSeconds(10)))
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(SECRET.getBytes(StandardCharsets.UTF_8)));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }
}
