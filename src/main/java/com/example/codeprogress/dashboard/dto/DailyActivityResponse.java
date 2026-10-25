package com.example.codeprogress.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Study time and session count for one calendar date")
public record DailyActivityResponse(
        LocalDate date,
        long studyMinutes,
        long studySessionsCount
) {
}
