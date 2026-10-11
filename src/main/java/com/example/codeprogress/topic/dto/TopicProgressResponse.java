package com.example.codeprogress.topic.dto;

import java.time.LocalDate;

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
