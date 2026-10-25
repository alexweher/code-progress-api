package com.example.codeprogress.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Public user information; password hashes are never exposed")
public record UserResponse(
        Long id,
        String email,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
