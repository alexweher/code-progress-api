package com.example.codeprogress.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Inclusive daily activity series, including days with zero activity")
public record DashboardActivityResponse(
        LocalDate from,
        LocalDate to,
        String timeZone,
        List<DailyActivityResponse> days
) {
}
