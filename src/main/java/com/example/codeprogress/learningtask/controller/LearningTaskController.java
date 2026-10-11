package com.example.codeprogress.learningtask.controller;

import com.example.codeprogress.learningtask.dto.CreateLearningTaskRequest;
import com.example.codeprogress.learningtask.dto.LearningTaskResponse;
import com.example.codeprogress.learningtask.dto.UpdateLearningTaskRequest;
import com.example.codeprogress.learningtask.dto.UpdateLearningTaskStatusRequest;
import com.example.codeprogress.learningtask.service.LearningTaskService;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class LearningTaskController {

    private final LearningTaskService learningTaskService;

    public LearningTaskController(LearningTaskService learningTaskService) {
        this.learningTaskService = learningTaskService;
    }

    @PostMapping("/topics/{topicId}/tasks")
    public ResponseEntity<LearningTaskResponse> create(
            @PathVariable Long topicId,
            @Valid @RequestBody CreateLearningTaskRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(learningTaskService.create(topicId, request));
    }

    @GetMapping("/topics/{topicId}/tasks")
    public List<LearningTaskResponse> getAllByTopicId(@PathVariable Long topicId) {
        return learningTaskService.getAllByTopicId(topicId);
    }

    @GetMapping("/tasks/{id}")
    public LearningTaskResponse getById(@PathVariable Long id) {
        return learningTaskService.getById(id);
    }

    @PutMapping("/tasks/{id}")
    public LearningTaskResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateLearningTaskRequest request
    ) {
        return learningTaskService.update(id, request);
    }

    @PatchMapping("/tasks/{id}/status")
    public LearningTaskResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateLearningTaskStatusRequest request
    ) {
        return learningTaskService.updateStatus(id, request);
    }

    @DeleteMapping("/tasks/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        learningTaskService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
