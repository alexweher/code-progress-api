package com.example.codeprogress.topic.controller;

import com.example.codeprogress.common.exception.GlobalExceptionHandler;
import com.example.codeprogress.topic.TopicStatus;
import com.example.codeprogress.topic.dto.CreateTopicRequest;
import com.example.codeprogress.topic.dto.TopicResponse;
import com.example.codeprogress.topic.dto.UpdateTopicRequest;
import com.example.codeprogress.topic.dto.UpdateTopicStatusRequest;
import com.example.codeprogress.topic.exception.TopicNotFoundException;
import com.example.codeprogress.topic.service.TopicService;
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

@WebMvcTest(TopicController.class)
@Import(GlobalExceptionHandler.class)
class TopicControllerTest {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    @MockitoBean
    private TopicService topicService;

    @Autowired
    TopicControllerTest(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    @Test
    void createsTopic() throws Exception {
        when(topicService.create(any(CreateTopicRequest.class))).thenReturn(topicResponse());

        mockMvc.perform(post("/api/topics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateTopicRequest("Java", "Core Java")
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Java"))
                .andExpect(jsonPath("$.status").value("PLANNED"));
    }

    @Test
    void rejectsBlankTitleOnCreate() throws Exception {
        mockMvc.perform(post("/api/topics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"   \",\"description\":\"Core Java\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("title"));
    }

    @Test
    void rejectsTooLongTitleOnCreate() throws Exception {
        CreateTopicRequest request = new CreateTopicRequest("a".repeat(121), null);

        mockMvc.perform(post("/api/topics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("title"));
    }

    @Test
    void rejectsTooLongDescriptionOnCreate() throws Exception {
        CreateTopicRequest request = new CreateTopicRequest("Java", "a".repeat(2001));

        mockMvc.perform(post("/api/topics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("description"));
    }

    @Test
    void returnsAllTopics() throws Exception {
        when(topicService.getAll()).thenReturn(List.of(topicResponse()));

        mockMvc.perform(get("/api/topics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Java"));
    }

    @Test
    void returnsTopicById() throws Exception {
        when(topicService.getById(1L)).thenReturn(topicResponse());

        mockMvc.perform(get("/api/topics/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void returnsNotFoundForUnknownTopic() throws Exception {
        when(topicService.getById(99L)).thenThrow(new TopicNotFoundException(99L));

        mockMvc.perform(get("/api/topics/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Topic with id 99 was not found"))
                .andExpect(jsonPath("$.path").value("/api/topics/99"));
    }

    @Test
    void updatesTopicDetails() throws Exception {
        TopicResponse updated = new TopicResponse(
                1L,
                "Modern Java",
                "Records and sealed classes",
                TopicStatus.IN_PROGRESS,
                topicResponse().createdAt(),
                topicResponse().updatedAt().plusHours(1)
        );
        when(topicService.update(any(Long.class), any(UpdateTopicRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/topics/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateTopicRequest("Modern Java", "Records and sealed classes")
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Modern Java"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void rejectsInvalidUpdateRequest() throws Exception {
        mockMvc.perform(put("/api/topics/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":null,\"description\":\"Description\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("title"));
    }

    @Test
    void updatesTopicStatus() throws Exception {
        TopicResponse updated = new TopicResponse(
                1L,
                "Java",
                "Core Java",
                TopicStatus.COMPLETED,
                topicResponse().createdAt(),
                topicResponse().updatedAt().plusHours(1)
        );
        when(topicService.updateStatus(any(Long.class), any(UpdateTopicStatusRequest.class)))
                .thenReturn(updated);

        mockMvc.perform(patch("/api/topics/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void rejectsMissingStatus() throws Exception {
        mockMvc.perform(patch("/api/topics/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("status"));
    }

    @Test
    void rejectsUnknownStatus() throws Exception {
        mockMvc.perform(patch("/api/topics/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"UNKNOWN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Request body is missing or contains invalid JSON"));
    }

    @Test
    void deletesTopic() throws Exception {
        doNothing().when(topicService).delete(1L);

        mockMvc.perform(delete("/api/topics/1"))
                .andExpect(status().isNoContent());

        verify(topicService).delete(1L);
    }

    @Test
    void returnsNotFoundWhenDeletingUnknownTopic() throws Exception {
        doThrow(new TopicNotFoundException(99L)).when(topicService).delete(99L);

        mockMvc.perform(delete("/api/topics/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void rejectsNonNumericTopicId() throws Exception {
        mockMvc.perform(get("/api/topics/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'id'"));
    }

    private TopicResponse topicResponse() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 22, 10, 0);
        return new TopicResponse(
                1L,
                "Java",
                "Core Java",
                TopicStatus.PLANNED,
                createdAt,
                createdAt
        );
    }
}
