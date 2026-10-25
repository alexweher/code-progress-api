package com.example.codeprogress.topic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Data required to create a learning topic")
public record CreateTopicRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 120, message = "Title must not exceed 120 characters")
        @Schema(example = "Java concurrency") String title,

        @Size(max = 2000, message = "Description must not exceed 2000 characters")
        @Schema(example = "Threads, executors and synchronization") String description
) {
}
