package com.app.backend.common.security;

/**
 * Principal đã xác thực — chứa userId, role và sid (sid dùng cho logout blacklist, xem 5.3/6.2 phase-1).
 */
public record AuthPrincipal(long userId, String role, String sid) {
}
