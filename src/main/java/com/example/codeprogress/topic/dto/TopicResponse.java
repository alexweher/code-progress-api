package com.example.codeprogress.topic.dto;

import com.example.codeprogress.topic.TopicStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Learning topic owned by the authenticated user")
public record TopicResponse(
        Long id,
        String title,
        String description,
        TopicStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
