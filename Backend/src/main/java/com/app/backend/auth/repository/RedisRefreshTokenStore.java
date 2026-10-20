package com.app.backend.auth.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

/**
 * Cấu trúc Redis theo D-1-02 (phase-1 mục 2.3):
 * auth:refresh:{jti} (String, 7 ngày) | auth:session:{sid} (Set, 7 ngày) | auth:blacklist:sid:{sid} (String, 15 phút).
 */
@Service
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {

    private final StringRedisTemplate redis;

    private static String refreshKey(String jti) {
        return "auth:refresh:" + jti;
    }

    private static String sessionKey(String sid) {
        return "auth:session:" + sid;
    }

    private static String blacklistKey(String sid) {
        return "auth:blacklist:sid:" + sid;
    }

    @Override
    public Optional<String> getAndDelete(String jti) {
        return Optional.ofNullable(redis.opsForValue().getAndDelete(refreshKey(jti)));
    }

    @Override
    public void saveRefreshToken(String jti, String sha256Hash, Duration ttl) {
        redis.opsForValue().set(refreshKey(jti), sha256Hash, ttl);
    }

    @Override
    public void deleteRefreshToken(String jti) {
        redis.delete(refreshKey(jti));
    }

    @Override
    public void addToSession(String sid, String jti, Duration ttl) {
        String key = sessionKey(sid);
        redis.opsForSet().add(key, jti);
        redis.expire(key, ttl);
    }

    @Override
    public void removeFromSession(String sid, String jti) {
        redis.opsForSet().remove(sessionKey(sid), jti);
    }

    @Override
    public Set<String> sessionMembers(String sid) {
        Set<String> members = redis.opsForSet().members(sessionKey(sid));
        return members == null ? Set.of() : members;
    }

    @Override
    public void deleteSession(String sid) {
        Set<String> jtis = sessionMembers(sid);
        if (!jtis.isEmpty()) {
            redis.delete(jtis.stream().map("auth:refresh:"::concat).toList());
        }
        redis.delete(sessionKey(sid));
    }

    @Override
    public boolean isBlacklisted(String sid) {
        return Boolean.TRUE.equals(redis.hasKey(blacklistKey(sid)));
    }

    @Override
    public void markRevoked(String sid, Duration ttl) {
        redis.opsForValue().set(blacklistKey(sid), "1", ttl);
    }
}
