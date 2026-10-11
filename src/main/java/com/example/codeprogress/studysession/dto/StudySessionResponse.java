package com.example.codeprogress.studysession.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

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
