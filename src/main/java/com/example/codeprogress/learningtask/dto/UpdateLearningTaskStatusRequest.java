package com.example.codeprogress.learningtask.dto;

import com.example.codeprogress.learningtask.LearningTaskStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateLearningTaskStatusRequest(
        @NotNull(message = "Status is required")
        LearningTaskStatus status
) {
}
