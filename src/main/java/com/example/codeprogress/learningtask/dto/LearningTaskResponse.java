package com.example.codeprogress.learningtask.dto;

import com.example.codeprogress.learningtask.LearningTaskDifficulty;
import com.example.codeprogress.learningtask.LearningTaskStatus;

import java.time.LocalDateTime;

public record LearningTaskResponse(
        Long id,
        Long topicId,
        String title,
        String description,
        LearningTaskDifficulty difficulty,
        LearningTaskStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
