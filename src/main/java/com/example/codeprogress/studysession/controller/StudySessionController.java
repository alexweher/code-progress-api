package com.example.codeprogress.studysession.controller;

import com.example.codeprogress.studysession.dto.CreateStudySessionRequest;
import com.example.codeprogress.studysession.dto.StudySessionResponse;
import com.example.codeprogress.studysession.dto.UpdateStudySessionRequest;
import com.example.codeprogress.studysession.service.StudySessionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class StudySessionController {

    private final StudySessionService studySessionService;

    public StudySessionController(StudySessionService studySessionService) {
        this.studySessionService = studySessionService;
    }

    @PostMapping("/topics/{topicId}/sessions")
    public ResponseEntity<StudySessionResponse> create(
            @PathVariable Long topicId,
            @Valid @RequestBody CreateStudySessionRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(studySessionService.create(topicId, request));
    }

    @GetMapping("/topics/{topicId}/sessions")
    public List<StudySessionResponse> getAllByTopicId(@PathVariable Long topicId) {
        return studySessionService.getAllByTopicId(topicId);
    }

    @GetMapping("/sessions/{id}")
    public StudySessionResponse getById(@PathVariable Long id) {
        return studySessionService.getById(id);
    }

    @PutMapping("/sessions/{id}")
    public StudySessionResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStudySessionRequest request
    ) {
        return studySessionService.update(id, request);
    }

    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        studySessionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
