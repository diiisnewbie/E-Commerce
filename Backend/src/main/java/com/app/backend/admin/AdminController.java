package com.app.backend.admin;

import com.app.backend.common.response.ApiResponse;
import com.app.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Admin endpoint mẫu (D-1-13) — chứng minh RBAC: chỉ ROLE_ADMIN truy cập được.
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;

    @GetMapping("/stats")
    public ApiResponse<Map<String, Long>> stats() {
        return ApiResponse.success(Map.of("totalUsers", userRepository.count()),
                "Admin stats retrieved successfully");
    }
}
