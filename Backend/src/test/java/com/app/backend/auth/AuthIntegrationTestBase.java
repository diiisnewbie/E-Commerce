package com.app.backend.auth;

import com.app.backend.auth.dto.LoginRequest;
import com.app.backend.auth.dto.RegisterRequest;
import com.app.backend.auth.repository.RefreshTokenStore;
import com.app.backend.common.BaseIntegrationTest;
import com.app.backend.common.security.JwtService;
import com.app.backend.user.UserRepository;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * Support dùng chung cho integration test Phase 1: helper HTTP, đăng ký/đăng nhập, ký token thủ công (TEST_SECRET).
 */
public abstract class AuthIntegrationTestBase extends BaseIntegrationTest {

    /** Khớp `app.jwt.secret` trong application-test.yml (test profile). */
    protected static final String TEST_SECRET = "test-only-jwt-secret-key-32-bytes-minimum-xx";

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected RefreshTokenStore refreshTokenStore;

    @Autowired
    protected JwtService jwtService;

    protected static HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    protected static HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = jsonHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return headers;
    }

    protected ResponseEntity<Map<String, Object>> postJson(String path, Object body) {
        return restTemplate.exchange(path, HttpMethod.POST, new HttpEntity<>(body, jsonHeaders()), mapType());
    }

    protected ResponseEntity<Map<String, Object>> postRawJson(String path, String rawBody) {
        return restTemplate.exchange(path, HttpMethod.POST, new HttpEntity<>(rawBody, jsonHeaders()), mapType());
    }

    protected ResponseEntity<Map<String, Object>> postWithBearer(String path, Object body, String token) {
        return restTemplate.exchange(path, HttpMethod.POST, new HttpEntity<>(body, bearerHeaders(token)), mapType());
    }

    protected ResponseEntity<Map<String, Object>> getWithBearer(String path, String token) {
        return restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(bearerHeaders(token)), mapType());
    }

    protected static org.springframework.core.ParameterizedTypeReference<Map<String, Object>> mapType() {
        return new org.springframework.core.ParameterizedTypeReference<>() {
        };
    }

    @SuppressWarnings("unchecked")
    protected static Map<String, Object> dataOf(ResponseEntity<Map<String, Object>> response) {
        return (Map<String, Object>) response.getBody().get("data");
    }

    protected static String errorCodeOf(ResponseEntity<Map<String, Object>> response) {
        return (String) response.getBody().get("errorCode");
    }

    protected static String messageOf(ResponseEntity<Map<String, Object>> response) {
        return (String) response.getBody().get("message");
    }

    /** Đăng ký thành công (assert 201) — trả data của response. */
    protected Map<String, Object> mustRegister(String email, String password) {
        ResponseEntity<Map<String, Object>> response = postJson("/api/v1/auth/register",
                new RegisterRequest(email, password, "Test User"));
        if (response.getStatusCode().value() != 201) {
            throw new IllegalStateException("Register không thành công: " + response.getBody());
        }
        return dataOf(response);
    }

    /** Đăng nhập thành công (assert 200) — trả TokenResponse data. */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> mustLogin(String email, String Password) {
        ResponseEntity<Map<String, Object>> response = postJson("/api/v1/auth/login",
                new LoginRequest(email, Password));
        if (response.getStatusCode().value() != 200) {
            throw new IllegalStateException("Login không thành công: " + response.getBody());
        }
        return dataOf(response);
    }

    protected static String accessTokenOf(Map<String, Object> tokenData) {
        return (String) tokenData.get("accessToken");
    }

    protected static String refreshTokenOf(Map<String, Object> tokenData) {
        return (String) tokenData.get("refreshToken");
    }

    protected static long userIdOf(Map<String, Object> tokenData) {
        @SuppressWarnings("unchecked")
        Map<String, Object> user = (Map<String, Object>) tokenData.get("user");
        return ((Number) user.get("id")).longValue();
    }

    protected String sidOf(String token) {
        return jwtService.decode(token).getClaimAsString("sid");
    }

    protected Jwt decode(String token) {
        return jwtService.decode(token);
    }

    /** Ký access token thủ công với exp tùy ý (test token hết hạn / blacklist — TEST_SECRET). */
    protected static String signAccessToken(long userId, String role, String sid, Instant exp) {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(Long.toString(userId))
                    .jwtID(UUID.randomUUID().toString())
                    .claim("sid", sid)
                    .claim("role", role)
                    .claim("type", "access")
                    .issueTime(Date.from(exp.minusSeconds(900)))
                    .expirationTime(Date.from(exp))
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(TEST_SECRET.getBytes(StandardCharsets.UTF_8)));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Không ký được token test", e);
        }
    }

    /** Token alg=none — luôn bị từ chối (TC-AUTH-24). */
    protected static String unsignedToken(long userId, String sid, Instant exp) {
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        // Payload JSON dựng tay (không dựa vào serialize() — version nimbus có thể không còn method này)
        String payloadJson = "{\"sub\":\"" + userId + "\",\"jti\":\"" + UUID.randomUUID()
                + "\",\"sid\":\"" + sid + "\",\"type\":\"access\",\"exp\":" + exp.getEpochSecond() + "}";
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".";
    }

    /** Token HS512 ký bằng key 64 byte — decoder chỉ chấp nhận HS256 nên phải bị từ chối (TC-AUTH-24). */
    protected static String hs512Token(long userId, String sid, Instant exp) {
        try {
            byte[] key64 = new byte[64];
            java.util.Arrays.fill(key64, (byte) 0x5A);
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(Long.toString(userId))
                    .jwtID(UUID.randomUUID().toString())
                    .claim("sid", sid)
                    .claim("type", "access")
                    .issueTime(Date.from(exp.minusSeconds(900)))
                    .expirationTime(Date.from(exp))
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS512), claims);
            jwt.sign(new MACSigner(key64));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Không ký được token HS512", e);
        }
    }

    protected static String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }
}
