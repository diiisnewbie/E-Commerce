package com.app.backend.common.security;

import com.app.backend.auth.repository.RefreshTokenStore;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

/**
 * Filter xác thực Bearer token (D-1-14): KHÔNG ghi response, KHÔNG chặn endpoint công khai.
 * Token hỏng → không authenticate + lưu mã lỗi vào request attribute; EntryPoint đọc attribute khi cần trả 401.
 * Đăng ký đúng một lần: không phải bean — được {@code SecurityConfig} new() và addFilterBefore (xem 6.2).
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String ERROR_ATTRIBUTE = "auth.token.error";

    private final JwtService jwtService;
    private final RefreshTokenStore refreshTokenStore;

    public JwtAuthenticationFilter(JwtService jwtService, RefreshTokenStore refreshTokenStore) {
        this.jwtService = jwtService;
        this.refreshTokenStore = refreshTokenStore;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = header.substring(7).trim();
        try {
            Jwt jwt = jwtService.decode(token);

            if (!JwtService.TYPE_ACCESS.equals(jwt.getClaimAsString("type"))) {
                request.setAttribute(ERROR_ATTRIBUTE, "TOKEN_INVALID");
            } else if (jwt.getExpiresAt() == null || !jwt.getExpiresAt().isAfter(Instant.now())) {
                request.setAttribute(ERROR_ATTRIBUTE, "TOKEN_EXPIRED");
            } else {
                String sid = jwt.getClaimAsString("sid");
                if (sid == null || refreshTokenStore.isBlacklisted(sid)) {
                    request.setAttribute(ERROR_ATTRIBUTE, "TOKEN_REVOKED");
                } else {
                    long userId = Long.parseLong(jwt.getSubject());
                    String role = jwt.getClaimAsString("role");
                    AuthPrincipal principal = new AuthPrincipal(userId, role, sid);
                    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                            principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
                }
            }
        } catch (JwtException | IllegalArgumentException e) {
            request.setAttribute(ERROR_ATTRIBUTE, "TOKEN_INVALID");
        }
        filterChain.doFilter(request, response);
    }
}
