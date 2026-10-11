package com.example.codeprogress.learningtask.dto;

import com.example.codeprogress.learningtask.LearningTaskDifficulty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateLearningTaskRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 160, message = "Title must not exceed 160 characters")
        String title,

        @Size(max = 3000, message = "Description must not exceed 3000 characters")
        String description,

        @NotNull(message = "Difficulty is required")
        LearningTaskDifficulty difficulty
) {
}
