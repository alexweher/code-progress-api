package com.example.codeprogress.learningtask.dto;

import com.example.codeprogress.learningtask.LearningTaskPriority;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record UpdateLearningTaskPlanningRequest(
        LocalDate deadline,

        @NotNull(message = "Priority is required")
        LearningTaskPriority priority
) {
}
