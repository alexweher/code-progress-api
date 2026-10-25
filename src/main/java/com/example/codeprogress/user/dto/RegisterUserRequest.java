package com.example.codeprogress.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "User registration credentials")
public record RegisterUserRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 320, message = "Email must not exceed 320 characters")
        @Schema(example = "student@example.com") String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must contain between 8 and 72 characters")
        @Schema(example = "change-this-password", accessMode = Schema.AccessMode.WRITE_ONLY) String password
) {
    public RegisterUserRequest {
        if (email != null) {
            email = email.trim();
        }
    }
}
