package com.app.backend.common.security;

import com.app.backend.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

/**
 * Trả 401 JSON theo ApiResponse — đọc mã lỗi do {@link JwtAuthenticationFilter} để lại trong attribute.
 * Chạy endpoint bắt buộc đăng nhập (permitAll không chạm tới).
 */
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final tools.jackson.databind.ObjectMapper objectMapper;

    public JsonAuthenticationEntryPoint(tools.jackson.databind.ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        String errorCode = (String) request.getAttribute(JwtAuthenticationFilter.ERROR_ATTRIBUTE);
        if (errorCode == null) {
            errorCode = "TOKEN_INVALID";
        }
        String message = switch (errorCode) {
            case "TOKEN_EXPIRED" -> "Access token has expired";
            case "TOKEN_REVOKED" -> "Session has been revoked";
            case "TOKEN_INVALID" -> request.getHeader(HttpHeaders.AUTHORIZATION) == null
                    ? "Authentication required"
                    : "Invalid access token";
            default -> "Authentication required";
        };
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(message, errorCode)));
    }
}
