package com.example.codeprogress.learningtask.dto;

import com.example.codeprogress.learningtask.LearningTaskDifficulty;
import com.example.codeprogress.learningtask.LearningTaskPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "Data required to create a learning task")
public record CreateLearningTaskRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 160, message = "Title must not exceed 160 characters")
        @Schema(example = "Implement optimistic locking example") String title,

        @Size(max = 3000, message = "Description must not exceed 3000 characters")
        String description,

        @NotNull(message = "Difficulty is required")
        @Schema(example = "MEDIUM") LearningTaskDifficulty difficulty,

        @Schema(description = "Optional deadline in the user's configured time zone", example = "2026-10-25")
        LocalDate deadline,

        @Schema(description = "Defaults to MEDIUM when omitted", example = "HIGH")
        LearningTaskPriority priority
) {
    public CreateLearningTaskRequest(
            String title,
            String description,
            LearningTaskDifficulty difficulty
    ) {
        this(title, description, difficulty, null, LearningTaskPriority.MEDIUM);
    }
}
