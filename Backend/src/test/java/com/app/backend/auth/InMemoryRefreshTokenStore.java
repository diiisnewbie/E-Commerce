package com.app.backend.auth;

import com.app.backend.auth.repository.RefreshTokenStore;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Fake Redis trong bộ nhớ — dùng cho unit test rotation (đã chốt O-1-05), không cần Redis.
 */
public class InMemoryRefreshTokenStore implements RefreshTokenStore {

    private final Map<String, String> tokens = new HashMap<>();
    private final Map<String, Set<String>> sessions = new HashMap<>();
    private final Set<String> blacklisted = new HashSet<>();

    @Override
    public Optional<String> getAndDelete(String jti) {
        return Optional.ofNullable(tokens.remove(jti));
    }

    @Override
    public void saveRefreshToken(String jti, String sha256Hash, Duration ttl) {
        tokens.put(jti, sha256Hash);
    }

    @Override
    public void deleteRefreshToken(String jti) {
        tokens.remove(jti);
    }

    @Override
    public void addToSession(String sid, String jti, Duration ttl) {
        sessions.computeIfAbsent(sid, k -> new HashSet<>()).add(jti);
    }

    @Override
    public void removeFromSession(String sid, String jti) {
        Set<String> members = sessions.get(sid);
        if (members != null) {
            members.remove(jti);
        }
    }

    @Override
    public Set<String> sessionMembers(String sid) {
        return Set.copyOf(sessions.getOrDefault(sid, Set.of()));
    }

    @Override
    public void deleteSession(String sid) {
        Set<String> members = sessions.remove(sid);
        if (members != null) {
            members.forEach(tokens::remove);
        }
    }

    @Override
    public boolean isBlacklisted(String sid) {
        return blacklisted.contains(sid);
    }

    @Override
    public void markRevoked(String sid, Duration ttl) {
        blacklisted.add(sid);
    }

    public int tokenCount() {
        return tokens.size();
    }

    public boolean hasSession(String sid) {
        return sessions.containsKey(sid);
    }
}
