package com.example.codeprogress.topic.controller;

import com.example.codeprogress.common.exception.ApiErrorResponse;
import com.example.codeprogress.topic.dto.TopicProgressResponse;
import com.example.codeprogress.topic.service.TopicProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/topics")
@Tag(name = "Topics")
public class TopicProgressController {

    private final TopicProgressService topicProgressService;

    public TopicProgressController(TopicProgressService topicProgressService) {
        this.topicProgressService = topicProgressService;
    }

    @GetMapping("/{topicId}/progress")
    @Operation(summary = "Get progress statistics for a topic")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Topic progress returned"),
            @ApiResponse(responseCode = "401", description = "Bearer token is missing or invalid",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Topic was not found for the current user",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public TopicProgressResponse getProgress(@PathVariable Long topicId) {
        return topicProgressService.getProgress(topicId);
    }
}
