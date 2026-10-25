package com.example.codeprogress.studysession.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Schema(description = "Recorded study session with completed task references")
public record StudySessionResponse(
        Long id,
        Long topicId,
        LocalDate sessionDate,
        int durationMinutes,
        String note,
        Set<Long> completedTaskIds,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
