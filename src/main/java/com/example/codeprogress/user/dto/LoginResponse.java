package com.example.codeprogress.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "JWT access token issued after successful login")
public record LoginResponse(
        @Schema(description = "Signed JWT", example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken,
        @Schema(example = "Bearer")
        String tokenType,
        @Schema(description = "Token lifetime in seconds", example = "3600")
        long expiresIn
) {
}
