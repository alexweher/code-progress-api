package com.example.codeprogress.learningtask.controller;

import com.example.codeprogress.common.exception.GlobalExceptionHandler;
import com.example.codeprogress.learningtask.LearningTaskDifficulty;
import com.example.codeprogress.learningtask.LearningTaskStatus;
import com.example.codeprogress.learningtask.dto.CreateLearningTaskRequest;
import com.example.codeprogress.learningtask.dto.LearningTaskResponse;
import com.example.codeprogress.learningtask.dto.UpdateLearningTaskRequest;
import com.example.codeprogress.learningtask.exception.LearningTaskNotFoundException;
import com.example.codeprogress.learningtask.service.LearningTaskService;
import com.example.codeprogress.topic.exception.TopicNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LearningTaskController.class)
@Import(GlobalExceptionHandler.class)
class LearningTaskControllerTest {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    @MockitoBean
    private LearningTaskService learningTaskService;

    @Autowired
    LearningTaskControllerTest(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    @Test
    void createsTask() throws Exception {
        when(learningTaskService.create(any(Long.class), any(CreateLearningTaskRequest.class)))
                .thenReturn(taskResponse());

        mockMvc.perform(post("/api/topics/1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateLearningTaskRequest("Build API", "Controllers", LearningTaskDifficulty.MEDIUM)
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.topicId").value(1))
                .andExpect(jsonPath("$.status").value("PLANNED"));
    }

    @Test
    void rejectsMissingDifficultyOnCreate() throws Exception {
        mockMvc.perform(post("/api/topics/1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Build API\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("difficulty"));
    }

    @Test
    void rejectsTooLongTitleOnCreate() throws Exception {
        CreateLearningTaskRequest request = new CreateLearningTaskRequest(
                "a".repeat(161), null, LearningTaskDifficulty.EASY
        );

        mockMvc.perform(post("/api/topics/1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("title"));
    }

    @Test
    void returnsTasksForTopic() throws Exception {
        when(learningTaskService.getAllByTopicId(1L)).thenReturn(List.of(taskResponse()));

        mockMvc.perform(get("/api/topics/1/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10));
    }

    @Test
    void returnsNotFoundForUnknownTopicWhenListingTasks() throws Exception {
        when(learningTaskService.getAllByTopicId(99L)).thenThrow(new TopicNotFoundException(99L));

        mockMvc.perform(get("/api/topics/99/tasks"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Topic with id 99 was not found"));
    }

    @Test
    void returnsTaskById() throws Exception {
        when(learningTaskService.getById(10L)).thenReturn(taskResponse());

        mockMvc.perform(get("/api/tasks/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void returnsNotFoundForUnknownTask() throws Exception {
        when(learningTaskService.getById(99L)).thenThrow(new LearningTaskNotFoundException(99L));

        mockMvc.perform(get("/api/tasks/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Learning task with id 99 was not found"));
    }

    @Test
    void updatesTask() throws Exception {
        LearningTaskResponse updated = new LearningTaskResponse(
                10L, 1L, "Updated", "Updated description", LearningTaskDifficulty.HARD,
                LearningTaskStatus.PLANNED, taskResponse().createdAt(), taskResponse().updatedAt()
        );
        when(learningTaskService.update(any(Long.class), any(UpdateLearningTaskRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/tasks/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateLearningTaskRequest("Updated", "Updated description", LearningTaskDifficulty.HARD)
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.difficulty").value("HARD"));
    }

    @Test
    void rejectsInvalidStatus() throws Exception {
        mockMvc.perform(patch("/api/tasks/10/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"UNKNOWN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void deletesTask() throws Exception {
        doNothing().when(learningTaskService).delete(10L);

        mockMvc.perform(delete("/api/tasks/10"))
                .andExpect(status().isNoContent());

        verify(learningTaskService).delete(10L);
    }

    @Test
    void returnsNotFoundWhenDeletingUnknownTask() throws Exception {
        doThrow(new LearningTaskNotFoundException(99L)).when(learningTaskService).delete(99L);

        mockMvc.perform(delete("/api/tasks/99"))
                .andExpect(status().isNotFound());
    }

    private LearningTaskResponse taskResponse() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 10, 11, 12, 0);
        return new LearningTaskResponse(
                10L, 1L, "Build API", "Controllers", LearningTaskDifficulty.MEDIUM,
                LearningTaskStatus.PLANNED, createdAt, createdAt
        );
    }
}
