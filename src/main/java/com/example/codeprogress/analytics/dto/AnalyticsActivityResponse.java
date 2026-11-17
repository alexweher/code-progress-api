package com.example.codeprogress.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Daily or ISO-weekly session activity, including zero-activity intervals")
public record AnalyticsActivityResponse(
        LocalDate from,
        LocalDate to,
        ActivityGrouping groupBy,
        String timeZone,
        List<Bucket> buckets
) {
    @Schema(description = "Inclusive intersection of a calendar day or ISO week with the requested period")
    public record Bucket(LocalDate start, LocalDate end, long studyMinutes, long studySessions) {
    }
}
