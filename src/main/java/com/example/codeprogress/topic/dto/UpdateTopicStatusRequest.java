package com.example.codeprogress.topic.dto;

import com.example.codeprogress.topic.TopicStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTopicStatusRequest(
        @NotNull(message = "Status is required")
        TopicStatus status
) {
}
