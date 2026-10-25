package com.example.codeprogress.studysession.controller;

import com.example.codeprogress.common.exception.ApiErrorResponse;
import com.example.codeprogress.common.pagination.PageRequestFactory;
import com.example.codeprogress.common.pagination.PageResponse;
import com.example.codeprogress.studysession.dto.CreateStudySessionRequest;
import com.example.codeprogress.studysession.dto.StudySessionResponse;
import com.example.codeprogress.studysession.dto.UpdateStudySessionRequest;
import com.example.codeprogress.studysession.service.StudySessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api")
@Tag(name = "Study sessions")
public class StudySessionController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "sessionDate", "durationMinutes", "createdAt", "updatedAt"
    );

    private final StudySessionService studySessionService;

    public StudySessionController(StudySessionService studySessionService) {
        this.studySessionService = studySessionService;
    }

    @PostMapping("/topics/{topicId}/sessions")
    @Operation(
            summary = "Create a study session",
            description = "Tasks listed as completed are marked COMPLETED atomically with the session."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Study session created"),
            @ApiResponse(responseCode = "400", description = "A completed task does not belong to the topic",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Topic or completed task was not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<StudySessionResponse> create(
            @PathVariable Long topicId,
            @Valid @RequestBody CreateStudySessionRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(studySessionService.create(topicId, request));
    }

    @GetMapping("/topics/{topicId}/sessions")
    @Operation(summary = "List all study sessions in a topic")
    public List<StudySessionResponse> getAllByTopicId(@PathVariable Long topicId) {
        return studySessionService.getAllByTopicId(topicId);
    }

    @GetMapping("/sessions/{id}")
    @Operation(summary = "Get a study session by id")
    public StudySessionResponse getById(@PathVariable Long id) {
        return studySessionService.getById(id);
    }

    @GetMapping("/sessions/search")
    @Operation(summary = "Search study sessions by topic and inclusive date range")
    public PageResponse<StudySessionResponse> search(
            @RequestParam(required = false) Long topicId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "sessionDate,desc") String sort
    ) {
        return PageResponse.from(studySessionService.search(
                topicId,
                dateFrom,
                dateTo,
                PageRequestFactory.create(page, size, sort, ALLOWED_SORT_FIELDS, "sessionDate")
        ));
    }

    @PutMapping("/sessions/{id}")
    @Operation(
            summary = "Update a study session",
            description = "Reconciles managed task statuses while preserving manual completion and other session references."
    )
    public StudySessionResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStudySessionRequest request
    ) {
        return studySessionService.update(id, request);
    }

    @DeleteMapping("/sessions/{id}")
    @Operation(
            summary = "Delete a study session",
            description = "Restores session-managed task status only when no other session still completes the task."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Study session deleted"),
            @ApiResponse(responseCode = "404", description = "Study session was not found for the current user",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        studySessionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
