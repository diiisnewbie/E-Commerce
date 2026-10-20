package com.app.backend.common.security;

import com.app.backend.user.Role;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.stereotype.Service;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Tạo/parse JWT HS256 (D-1-01, D-1-15).
 * Decoder KHÔNG dùng JwtTimestampValidator (tránh clock skew 60s) — filter/service tự kiểm exp.
 */
@Service
public class JwtService {

    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";
    public static final long ACCESS_TTL_SECONDS = 15 * 60;
    public static final long REFRESH_TTL_SECONDS = 7 * 24 * 60 * 60;

    private final SecretKeySpec secretKey;
    private final NimbusJwtDecoder decoder;

    public JwtService(@Value("${app.jwt.secret}") String secret) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            // Fail-fast (UT-03): HS256 yêu cầu key ≥ 256 bit
            throw new IllegalArgumentException("app.jwt.secret phải có ít nhất 32 byte UTF-8");
        }
        this.secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");
        this.decoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256)
                .build();
        this.decoder.setJwtValidator(jwt -> OAuth2TokenValidatorResult.success());
    }

    public record IssuedToken(String token, String jti) {
    }

    public IssuedToken issueAccessToken(long userId, Role role, String sid) {
        return issue(userId, sid, TYPE_ACCESS, role.name(), ACCESS_TTL_SECONDS);
    }

    public IssuedToken issueRefreshToken(long userId, String sid) {
        return issue(userId, sid, TYPE_REFRESH, null, REFRESH_TTL_SECONDS);
    }

    private IssuedToken issue(long userId, String sid, String type, String role, long ttlSeconds) {
        String jti = UUID.randomUUID().toString();
        Instant now = Instant.now();
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .subject(Long.toString(userId))
                .jwtID(jti)
                .claim("sid", sid)
                .claim("type", type)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(ttlSeconds)));
        if (role != null) {
            builder.claim("role", role);
        }
        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), builder.build());
            jwt.sign(new MACSigner(secretKey));
            return new IssuedToken(jwt.serialize(), jti);
        } catch (JOSEException e) {
            throw new IllegalStateException("Không tạo được JWT", e);
        }
    }

    /**
     * Verify chữ ký rồi trả claims — KHÔNG kiểm timestamp (D-1-15).
     * Ném {@link JwtException} nếu sai chữ ký / sai cấu trúc.
     */
    public Jwt decode(String token) {
        return decoder.decode(token);
    }
}
