package com.example.codeprogress.learningtask.dto;

import com.example.codeprogress.learningtask.LearningTaskDifficulty;
import com.example.codeprogress.learningtask.LearningTaskPriority;
import com.example.codeprogress.learningtask.LearningTaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "Learning task belonging to a topic owned by the authenticated user")
public record LearningTaskResponse(
        Long id,
        Long topicId,
        String title,
        String description,
        LearningTaskDifficulty difficulty,
        LearningTaskStatus status,
        LocalDate deadline,
        LearningTaskPriority priority,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public LearningTaskResponse(
            Long id,
            Long topicId,
            String title,
            String description,
            LearningTaskDifficulty difficulty,
            LearningTaskStatus status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this(
                id,
                topicId,
                title,
                description,
                difficulty,
                status,
                null,
                LearningTaskPriority.MEDIUM,
                createdAt,
                updatedAt
        );
    }
}
