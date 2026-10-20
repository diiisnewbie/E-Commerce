package com.app.backend.auth;

import com.app.backend.auth.dto.LoginRequest;
import com.app.backend.auth.dto.RefreshRequest;
import com.app.backend.auth.dto.TokenResponse;
import com.app.backend.auth.exception.AuthException;
import com.app.backend.auth.service.AuthService;
import com.app.backend.common.security.JwtService;
import com.app.backend.user.Role;
import com.app.backend.user.User;
import com.app.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test rotation của AuthService với fake RefreshTokenStore (đã chốt O-1-05) — không cần Redis.
 */
class AuthServiceRefreshRotationTest {

    private static final String SECRET = "unit-test-jwt-secret-32-bytes-minimum-abc";

    private final JwtService jwtService = new JwtService(SECRET);
    private final InMemoryRefreshTokenStore refreshTokenStore = new InMemoryRefreshTokenStore();
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final AuthService authService =
            new AuthService(userRepository, passwordEncoder, jwtService, refreshTokenStore);

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(7L);
        user.setEmail("unit@example.com");
        user.setPasswordHash(passwordEncoder.encode("MatKhau123"));
        user.setFullName("Unit Test");
        user.setRole(Role.USER);
        user.setActive(true);
        when(userRepository.findByEmail("unit@example.com")).thenReturn(Optional.of(user));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
    }

    @Test
    void TC_AUTH_UT_04_rotate_thanh_cong() {
        TokenResponse first = authService.login(new LoginRequest("unit@example.com", "MatKhau123"));
        assertThat(refreshTokenStore.tokenCount()).isEqualTo(1);

        TokenResponse rotated = authService.refresh(new RefreshRequest(first.refreshToken()));

        assertThat(rotated.refreshToken()).isNotEqualTo(first.refreshToken());
        assertThat(rotated.accessToken()).isNotEqualTo(first.accessToken());
        assertThat(rotated.user().id()).isEqualTo(7L);

        String oldJti = jwtService.decode(first.refreshToken()).getClaimAsString("jti");
        String newJti = jwtService.decode(rotated.refreshToken()).getClaimAsString("jti");
        String sid = jwtService.decode(rotated.refreshToken()).getClaimAsString("sid");
        assertThat(oldJti).isNotEqualTo(newJti);
        // Jti cũ đã bị xóa, jti mới được ghi — session giữ đúng 1 token
        assertThat(refreshTokenStore.tokenCount()).isEqualTo(1);
        assertThat(refreshTokenStore.sessionMembers(sid)).containsExactly(newJti);
        assertThat(refreshTokenStore.isBlacklisted(sid)).isFalse();
    }

    @Test
    void TC_AUTH_UT_04_dung_lai_thu_hoi_phien() {
        TokenResponse first = authService.login(new LoginRequest("unit@example.com", "MatKhau123"));
        authService.refresh(new RefreshRequest(first.refreshToken()));
        String sid = jwtService.decode(first.refreshToken()).getClaimAsString("sid");

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest(first.refreshToken())))
                .isInstanceOf(AuthException.class)
                .satisfies(ex -> assertThat(((AuthException) ex).getErrorCode()).isEqualTo("TOKEN_REVOKED"));

        assertThat(refreshTokenStore.isBlacklisted(sid)).isTrue();
        assertThat(refreshTokenStore.hasSession(sid)).isFalse();
    }
}
