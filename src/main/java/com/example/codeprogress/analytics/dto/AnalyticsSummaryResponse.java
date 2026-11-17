package com.example.codeprogress.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Owner-scoped learning summary. Task counts are a current snapshot, independent of the date range.")
public record AnalyticsSummaryResponse(
        LocalDate from,
        LocalDate to,
        String timeZone,
        AllTimeStudy allTime,
        PeriodStudy period,
        TasksSnapshot tasksSnapshot
) {
    public record AllTimeStudy(long studyMinutes, long studySessions, long activeDays, LocalDate lastStudyDate) {
    }

    @Schema(description = "Session totals for the inclusive from/to date range")
    public record PeriodStudy(long studyMinutes, long studySessions, long activeDays) {
    }

    @Schema(description = "Current task statuses, not historical completions within the date range")
    public record TasksSnapshot(
            long total,
            long planned,
            long inProgress,
            long completed,
            int completionPercent
    ) {
    }
}
