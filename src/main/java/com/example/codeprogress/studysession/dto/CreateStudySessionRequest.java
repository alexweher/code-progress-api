package com.example.codeprogress.studysession.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.Set;

@Schema(description = "Study session and tasks completed during it")
public record CreateStudySessionRequest(
        @NotNull(message = "Session date is required")
        @Schema(example = "2026-10-20") LocalDate sessionDate,

        @Positive(message = "Duration must be greater than zero")
        @Schema(example = "45") int durationMinutes,

        @Size(max = 2000, message = "Note must not exceed 2000 characters")
        String note,

        @NotNull(message = "Completed task ids are required")
        @Schema(description = "Task ids must belong to the same topic and current user", example = "[1, 2]")
        Set<@NotNull(message = "Completed task id is required")
                @Positive(message = "Completed task id must be greater than zero") Long> completedTaskIds
) {
}
