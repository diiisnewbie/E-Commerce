package com.app.backend.auth.repository;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

/**
 * Interface lưu token/phiên (đã chốt O-1-05) — implementation Redis ở {@link RedisRefreshTokenStore};
 * unit test rotation dùng fake implementation, không cần Redis.
 */
public interface RefreshTokenStore {

    /** GETDEL auth:refresh:{jti} — nguyên tử đọc+xóa (D-1-03). */
    Optional<String> getAndDelete(String jti);

    void saveRefreshToken(String jti, String sha256Hash, Duration ttl);

    void deleteRefreshToken(String jti);

    void addToSession(String sid, String jti, Duration ttl);

    void removeFromSession(String sid, String jti);

    Set<String> sessionMembers(String sid);

    /** Xóa mọi refresh token của phiên + session set (không đụng blacklist). */
    void deleteSession(String sid);

    boolean isBlacklisted(String sid);

    void markRevoked(String sid, Duration ttl);
}
