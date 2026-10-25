package com.example.codeprogress.common.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Standard error response returned by request validation and exception handling")
public record ApiErrorResponse(
        @Schema(example = "2026-10-20T12:00:00")
        LocalDateTime timestamp,
        @Schema(example = "400")
        int status,
        @Schema(example = "Bad Request")
        String error,
        @Schema(example = "Request validation failed")
        String message,
        @Schema(example = "/api/topics")
        String path,
        List<FieldValidationError> fieldErrors
) {
    @Schema(description = "Validation error for one request field")
    public record FieldValidationError(String field, String message) {
    }
}
