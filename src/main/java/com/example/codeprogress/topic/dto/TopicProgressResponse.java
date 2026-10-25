package com.example.codeprogress.topic.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Aggregated task and study-session progress for one topic")
public record TopicProgressResponse(
        Long topicId,
        long totalTasks,
        long completedTasks,
        int progressPercent,
        long totalStudyMinutes,
        long studySessionsCount,
        LocalDate lastStudyDate
) {
}
