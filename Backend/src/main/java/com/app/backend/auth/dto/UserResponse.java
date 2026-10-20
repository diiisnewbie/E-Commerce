package com.app.backend.auth.dto;

public record UserResponse(Long id, String email, String fullName, String role) {
}
