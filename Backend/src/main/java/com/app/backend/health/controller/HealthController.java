package com.app.backend.health.controller;

import com.app.backend.common.response.ApiResponse;
import com.app.backend.health.dto.HealthResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    private static final String SERVICE_NAME = "ecommerce-backend";

    @GetMapping
    public ApiResponse<HealthResponse> health() {
        return ApiResponse.success(
                new HealthResponse("UP", SERVICE_NAME),
                "Service is healthy");
    }
}
