package com.example.codeprogress.studysession.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Set;

public record CreateStudySessionRequest(
        @NotNull(message = "Session date is required")
        LocalDate sessionDate,

        @Positive(message = "Duration must be greater than zero")
        int durationMinutes,

        @Size(max = 2000, message = "Note must not exceed 2000 characters")
        String note,

        @NotNull(message = "Completed task ids are required")
        Set<Long> completedTaskIds
) {
}
