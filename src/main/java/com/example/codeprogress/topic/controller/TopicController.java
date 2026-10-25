package com.example.codeprogress.topic.controller;

import com.example.codeprogress.common.exception.ApiErrorResponse;
import com.example.codeprogress.common.pagination.PageRequestFactory;
import com.example.codeprogress.common.pagination.PageResponse;
import com.example.codeprogress.topic.dto.CreateTopicRequest;
import com.example.codeprogress.topic.dto.TopicResponse;
import com.example.codeprogress.topic.dto.UpdateTopicRequest;
import com.example.codeprogress.topic.dto.UpdateTopicStatusRequest;
import com.example.codeprogress.topic.service.TopicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/topics")
@Tag(name = "Topics")
public class TopicController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "title", "status", "createdAt", "updatedAt"
    );

    private final TopicService topicService;

    public TopicController(TopicService topicService) {
        this.topicService = topicService;
    }

    @PostMapping
    @Operation(summary = "Create a topic for the current user")
    @ApiResponse(responseCode = "201", description = "Topic created")
    public ResponseEntity<TopicResponse> create(@Valid @RequestBody CreateTopicRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(topicService.create(request));
    }

    @GetMapping
    @Operation(summary = "List all topics owned by the current user")
    public List<TopicResponse> getAll() {
        return topicService.getAll();
    }

    @GetMapping("/search")
    @Operation(summary = "Search topics with pagination and sorting")
    public PageResponse<TopicResponse> search(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "") String sort
    ) {
        return PageResponse.from(topicService.search(
                PageRequestFactory.create(page, size, sort, ALLOWED_SORT_FIELDS, "id")
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a topic by id")
    public TopicResponse getById(@PathVariable Long id) {
        return topicService.getById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace editable topic details")
    public TopicResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTopicRequest request
    ) {
        return topicService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Change a topic status")
    public TopicResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTopicStatusRequest request
    ) {
        return topicService.updateStatus(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an empty topic")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Topic deleted"),
            @ApiResponse(responseCode = "404", description = "Topic was not found for the current user",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Topic has learning tasks or study sessions",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        topicService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
