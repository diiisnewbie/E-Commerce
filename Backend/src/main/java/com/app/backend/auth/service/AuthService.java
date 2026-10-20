package com.app.backend.auth.service;

import com.app.backend.auth.dto.LoginRequest;
import com.app.backend.auth.dto.RegisterRequest;
import com.app.backend.auth.dto.RefreshRequest;
import com.app.backend.auth.dto.TokenResponse;
import com.app.backend.auth.dto.UserResponse;
import com.app.backend.auth.exception.AuthException;
import com.app.backend.auth.repository.RefreshTokenStore;
import com.app.backend.common.security.AuthPrincipal;
import com.app.backend.common.security.JwtService;
import com.app.backend.user.Role;
import com.app.backend.user.User;
import com.app.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Luồng register/login/refresh/logout/me — đầy đủ các bước trong 2.3 phase-1.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    /**
     * Hash BCrypt cố định của một mật khẩu ngẫu nhiên — chỉ dùng để "nuôi" thời gian khi email không tồn tại
     * hoặc user bị khóa (B11 chống dò tài khoản); không phải secret.
     */
    private static final String DUMMY_HASH =
            "$2a$10$VovY6sfo4zrQrHZck5.H6O.YLfBz3qKQHcxgH0X2knbHQhHrhjA5u";

    private static final Duration REFRESH_TTL = Duration.ofSeconds(JwtService.REFRESH_TTL_SECONDS);
    private static final Duration BLACKLIST_TTL = Duration.ofSeconds(JwtService.ACCESS_TTL_SECONDS);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenStore refreshTokenStore;

    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        validatePasswordBytes(request.password());
        if (userRepository.existsByEmail(email)) {
            throw AuthException.emailExists();
        }
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        user.setRole(Role.USER);
        user.setActive(true);
        // saveAndFlush: DataIntegrityViolationException ném ngay tại service (race hai register cùng email)
        userRepository.saveAndFlush(user);
        return toUserResponse(user);
    }

    public TokenResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        validatePasswordBytes(request.password());
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null || !user.isActive()) {
            // Vẫn chạy matches với dummy hash để thời gian xử lý gần bằng sai mật khẩu (B11)
            passwordEncoder.matches(request.password(), DUMMY_HASH);
            throw AuthException.invalidCredentials();
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw AuthException.invalidCredentials();
        }
        return issueTokens(user, UUID.randomUUID().toString());
    }

    public TokenResponse refresh(RefreshRequest request) {
        Jwt jwt = decodeRefreshOrThrow(request.refreshToken());

        // Bước 2: kiểm type — sai type trả TOKEN_INVALID ngay, không chạm Redis, không revoke
        if (!JwtService.TYPE_REFRESH.equals(jwt.getClaimAsString("type"))) {
            throw AuthException.token("TOKEN_INVALID", "Invalid token type");
        }
        String jti = jwt.getClaimAsString("jti");
        String sid = jwt.getClaimAsString("sid");
        if (jti == null || sid == null) {
            throw AuthException.token("TOKEN_INVALID", "Invalid refresh token");
        }

        // Bước 3: GETDEL nguyên tử — null nghĩa là token đã bị dùng
        Optional<String> storedHash = refreshTokenStore.getAndDelete(jti);
        if (storedHash.isEmpty()) {
            revokeSession(sid);
            throw AuthException.token("TOKEN_REVOKED", "Refresh token has been reused");
        }

        // Bước 4: hash không khớp → TOKEN_INVALID, không thu hồi phiên (đã chốt Q-1-03)
        if (!storedHash.get().equals(sha256Hex(request.refreshToken()))) {
            throw AuthException.token("TOKEN_INVALID", "Invalid refresh token");
        }

        // Bước 5: kiểm blacklist trước khi cấp token mới
        if (refreshTokenStore.isBlacklisted(sid)) {
            throw AuthException.token("TOKEN_REVOKED", "Session has been revoked");
        }

        // Bước 6: query user theo sub — xóa/khóa → thu hồi phiên (đã chốt Q-1-02); role lấy từ DB
        long userId = Long.parseLong(jwt.getSubject());
        Optional<User> found = userRepository.findById(userId);
        if (found.isEmpty() || !found.get().isActive()) {
            revokeSession(sid);
            throw AuthException.token("TOKEN_REVOKED", "Session has been revoked");
        }
        User user = found.get();

        // Bước 7: xóa jti cũ khỏi session, cấp token mới (cùng sid), ghi key mới + TTL 7 ngày
        refreshTokenStore.removeFromSession(sid, jti);
        TokenResponse tokens = issueTokens(user, sid);

        // Bước 8: kiểm lại blacklist sau khi ghi — logout race trong lúc refresh
        if (refreshTokenStore.isBlacklisted(sid)) {
            refreshTokenStore.deleteSession(sid);
            throw AuthException.token("TOKEN_REVOKED", "Session has been revoked");
        }
        return tokens;
    }

    public void logout(AuthPrincipal principal) {
        revokeSession(principal.sid());
    }

    public UserResponse me(AuthPrincipal principal) {
        User user = userRepository.findById(principal.userId())
                .orElseThrow(() -> AuthException.token("TOKEN_INVALID", "User no longer exists"));
        return toUserResponse(user);
    }

    private TokenResponse issueTokens(User user, String sid) {
        JwtService.IssuedToken access = jwtService.issueAccessToken(user.getId(), user.getRole(), sid);
        JwtService.IssuedToken refresh = jwtService.issueRefreshToken(user.getId(), sid);
        refreshTokenStore.saveRefreshToken(refresh.jti(), sha256Hex(refresh.token()), REFRESH_TTL);
        refreshTokenStore.addToSession(sid, refresh.jti(), REFRESH_TTL);
        return new TokenResponse(access.token(), refresh.token(), "Bearer",
                JwtService.ACCESS_TTL_SECONDS, toUserResponse(user));
    }

    private void revokeSession(String sid) {
        refreshTokenStore.deleteSession(sid);
        refreshTokenStore.markRevoked(sid, BLACKLIST_TTL);
    }

    private Jwt decodeRefreshOrThrow(String token) {
        Jwt jwt;
        try {
            jwt = jwtService.decode(token);
        } catch (JwtException e) {
            throw AuthException.token("TOKEN_INVALID", "Invalid refresh token");
        }
        if (jwt.getExpiresAt() == null || !jwt.getExpiresAt().isAfter(Instant.now())) {
            throw AuthException.token("TOKEN_EXPIRED", "Refresh token has expired");
        }
        return jwt;
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static void validatePasswordBytes(String password) {
        int bytes = password == null ? 0 : password.getBytes(StandardCharsets.UTF_8).length;
        if (bytes < 8 || bytes > 72) {
            throw AuthException.validation("Password must be 8-72 bytes");
        }
    }

    private static UserResponse toUserResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole().name());
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 không khả dụng", e);
        }
    }
}
