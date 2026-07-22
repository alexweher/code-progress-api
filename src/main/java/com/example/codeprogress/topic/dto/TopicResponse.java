package com.example.codeprogress.topic.dto;

import com.example.codeprogress.topic.TopicStatus;

import java.time.LocalDateTime;

public record TopicResponse(
        Long id,
        String title,
        String description,
        TopicStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
