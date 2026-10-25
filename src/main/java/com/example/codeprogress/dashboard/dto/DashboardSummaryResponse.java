package com.example.codeprogress.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Owner-scoped dashboard totals and recent study statistics")
public record DashboardSummaryResponse(
        long totalTopics,
        long plannedTasks,
        long activeTasks,
        long completedTasks,
        long totalStudyMinutes,
        long studyMinutesLast7Days,
        long studySessionsCount,
        @Schema(description = "Rounded completed-task percentage; zero when there are no tasks") int completionPercent,
        LocalDate lastStudyDate
) {
}
