package com.example.codeprogress.learningtask.controller;

import com.example.codeprogress.common.exception.ApiErrorResponse;
import com.example.codeprogress.common.pagination.PageRequestFactory;
import com.example.codeprogress.common.pagination.PageResponse;
import com.example.codeprogress.learningtask.LearningTaskDifficulty;
import com.example.codeprogress.learningtask.LearningTaskPriority;
import com.example.codeprogress.learningtask.LearningTaskStatus;
import com.example.codeprogress.learningtask.dto.CreateLearningTaskRequest;
import com.example.codeprogress.learningtask.dto.LearningTaskResponse;
import com.example.codeprogress.learningtask.dto.UpdateLearningTaskRequest;
import com.example.codeprogress.learningtask.dto.UpdateLearningTaskPlanningRequest;
import com.example.codeprogress.learningtask.dto.UpdateLearningTaskStatusRequest;
import com.example.codeprogress.learningtask.service.LearningTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
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
@Tag(name = "Learning tasks")
public class LearningTaskController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "title", "status", "difficulty", "priority", "deadline", "createdAt", "updatedAt"
    );

    private final LearningTaskService learningTaskService;

    public LearningTaskController(LearningTaskService learningTaskService) {
        this.learningTaskService = learningTaskService;
    }

    @PostMapping("/topics/{topicId}/tasks")
    @Operation(summary = "Create a learning task in a topic")
    @ApiResponse(responseCode = "201", description = "Learning task created")
    public ResponseEntity<LearningTaskResponse> create(
            @PathVariable Long topicId,
            @Valid @RequestBody CreateLearningTaskRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(learningTaskService.create(topicId, request));
    }

    @GetMapping("/topics/{topicId}/tasks")
    @Operation(summary = "List all tasks in a topic")
    public List<LearningTaskResponse> getAllByTopicId(@PathVariable Long topicId) {
        return learningTaskService.getAllByTopicId(topicId);
    }

    @GetMapping("/tasks/search")
    @Operation(summary = "Search tasks with owner-scoped filters, pagination and sorting")
    public PageResponse<LearningTaskResponse> search(
            @RequestParam(required = false) Long topicId,
            @RequestParam(required = false) LearningTaskStatus status,
            @RequestParam(required = false) LearningTaskDifficulty difficulty,
            @RequestParam(required = false) LearningTaskPriority priority,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deadlineFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deadlineTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "") String sort
    ) {
        return PageResponse.from(learningTaskService.search(
                topicId,
                status,
                difficulty,
                priority,
                deadlineFrom,
                deadlineTo,
                PageRequestFactory.create(page, size, sort, ALLOWED_SORT_FIELDS, "id")
        ));
    }

    @GetMapping("/tasks/{id}")
    @Operation(summary = "Get a learning task by id")
    public LearningTaskResponse getById(@PathVariable Long id) {
        return learningTaskService.getById(id);
    }

    @PutMapping("/tasks/{id}")
    @Operation(summary = "Replace editable task details")
    public LearningTaskResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateLearningTaskRequest request
    ) {
        return learningTaskService.update(id, request);
    }

    @PatchMapping("/tasks/{id}/status")
    @Operation(summary = "Change a task status manually")
    public LearningTaskResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateLearningTaskStatusRequest request
    ) {
        return learningTaskService.updateStatus(id, request);
    }

    @PatchMapping("/tasks/{id}/planning")
    @Operation(summary = "Change a task deadline and priority")
    public LearningTaskResponse updatePlanning(
            @PathVariable Long id,
            @Valid @RequestBody UpdateLearningTaskPlanningRequest request
    ) {
        return learningTaskService.updatePlanning(id, request);
    }

    @GetMapping("/tasks/due-today")
    @Operation(summary = "List incomplete tasks due today")
    public PageResponse<LearningTaskResponse> getDueToday(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "deadline,asc") String sort
    ) {
        return PageResponse.from(learningTaskService.getDueToday(
                PageRequestFactory.create(page, size, sort, ALLOWED_SORT_FIELDS, "deadline")
        ));
    }

    @GetMapping("/tasks/upcoming")
    @Operation(summary = "List incomplete tasks due during the inclusive seven-day window starting today")
    public PageResponse<LearningTaskResponse> getUpcoming(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "deadline,asc") String sort
    ) {
        return PageResponse.from(learningTaskService.getUpcoming(
                PageRequestFactory.create(page, size, sort, ALLOWED_SORT_FIELDS, "deadline")
        ));
    }

    @GetMapping("/tasks/overdue")
    @Operation(summary = "List incomplete tasks with a deadline before today")
    public PageResponse<LearningTaskResponse> getOverdue(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "deadline,asc") String sort
    ) {
        return PageResponse.from(learningTaskService.getOverdue(
                PageRequestFactory.create(page, size, sort, ALLOWED_SORT_FIELDS, "deadline")
        ));
    }

    @DeleteMapping("/tasks/{id}")
    @Operation(summary = "Delete a task that is not referenced by study history")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Learning task deleted"),
            @ApiResponse(responseCode = "404", description = "Task was not found for the current user",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Task is referenced by study history",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        learningTaskService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
