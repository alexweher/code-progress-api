package com.example.codeprogress.studysession.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Set;

public record UpdateStudySessionRequest(
        @NotNull(message = "Session date is required")
        LocalDate sessionDate,

        @Positive(message = "Duration must be greater than zero")
        int durationMinutes,

        @Size(max = 2000, message = "Note must not exceed 2000 characters")
        String note,

        @NotNull(message = "Completed task ids are required")
        Set<@NotNull(message = "Completed task id is required")
                @Positive(message = "Completed task id must be greater than zero") Long> completedTaskIds
) {
}
