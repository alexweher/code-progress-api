package com.example.codeprogress.analytics.dto;

import com.example.codeprogress.topic.TopicStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Current task progress and period study activity for every topic owned by the user")
public record AnalyticsTopicsResponse(
        LocalDate from,
        LocalDate to,
        String timeZone,
        List<Topic> topics
) {
    public record Topic(
            Long topicId,
            String title,
            TopicStatus status,
            @Schema(description = "Current task statuses, independent of the requested period") Tasks tasks,
            @Schema(description = "Study sessions within the inclusive requested period") PeriodStudy periodStudy
    ) {
    }

    public record Tasks(long total, long planned, long inProgress, long completed, int completionPercent) {
    }

    public record PeriodStudy(long studyMinutes, long studySessions, long activeDays, LocalDate lastStudyDate) {
    }
}
