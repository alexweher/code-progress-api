package com.example.codeprogress.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Credentials used to issue an access token")
public record LoginRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 320, message = "Email must not exceed 320 characters")
        @Schema(example = "student@example.com") String email,

        @NotBlank(message = "Password is required")
        @Schema(example = "change-this-password", accessMode = Schema.AccessMode.WRITE_ONLY) String password
) {
    public LoginRequest {
        if (email != null) {
            email = email.trim();
        }
    }
}
