package com.app.backend.common.security;

import com.app.backend.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

/**
 * Trả 403 JSON theo ApiResponse (đã xác thực nhưng thiếu vai trò).
 */
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private final tools.jackson.databind.ObjectMapper objectMapper;

    public JsonAccessDeniedHandler(tools.jackson.databind.ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(
                ApiResponse.error("Access denied", "ACCESS_DENIED")));
    }
}
