package com.example.codeprogress.studysession.controller;

import com.example.codeprogress.common.exception.GlobalExceptionHandler;
import com.example.codeprogress.studysession.dto.CreateStudySessionRequest;
import com.example.codeprogress.studysession.dto.StudySessionResponse;
import com.example.codeprogress.studysession.dto.UpdateStudySessionRequest;
import com.example.codeprogress.studysession.exception.LearningTaskDoesNotBelongToTopicException;
import com.example.codeprogress.studysession.exception.StudySessionNotFoundException;
import com.example.codeprogress.studysession.service.StudySessionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudySessionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class StudySessionControllerTest {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    @MockitoBean
    private StudySessionService studySessionService;

    @Autowired
    StudySessionControllerTest(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    @Test
    void createsSession() throws Exception {
        when(studySessionService.create(any(Long.class), any(CreateStudySessionRequest.class)))
                .thenReturn(sessionResponse());

        mockMvc.perform(post("/api/topics/1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateStudySessionRequest(LocalDate.of(2026, 10, 11), 60, "Practice", Set.of(10L))
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.completedTaskIds[0]").value(10));
    }

    @Test
    void rejectsZeroDuration() throws Exception {
        mockMvc.perform(post("/api/topics/1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionDate\":\"2026-10-11\",\"durationMinutes\":0,\"completedTaskIds\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("durationMinutes"));
    }

    @Test
    void rejectsNonPositiveCompletedTaskId() throws Exception {
        mockMvc.perform(post("/api/topics/1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionDate\":\"2026-10-11\",\"durationMinutes\":60,\"completedTaskIds\":[0]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.fieldErrors[*].message",
                        hasItem("Completed task id must be greater than zero")));
    }

    @Test
    void returnsSessionsForTopic() throws Exception {
        when(studySessionService.getAllByTopicId(1L)).thenReturn(List.of(sessionResponse()));

        mockMvc.perform(get("/api/topics/1/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].durationMinutes").value(60));
    }

    @Test
    void returnsFilteredSessionPage() throws Exception {
        when(studySessionService.search(any(), any(), any(), any())).thenReturn(
                new PageImpl<>(List.of(sessionResponse()), PageRequest.of(0, 10), 1)
        );

        mockMvc.perform(get("/api/sessions/search")
                        .param("topicId", "1")
                        .param("dateFrom", "2026-10-01")
                        .param("dateTo", "2026-10-31")
                        .param("size", "10")
                        .param("sort", "sessionDate,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(5))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(studySessionService).search(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(LocalDate.of(2026, 10, 1)),
                org.mockito.ArgumentMatchers.eq(LocalDate.of(2026, 10, 31)),
                any()
        );
    }

    @Test
    void rejectsInvalidSessionDateFilter() throws Exception {
        mockMvc.perform(get("/api/sessions/search").param("dateFrom", "not-a-date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'dateFrom'"));
    }

    @Test
    void returnsSessionById() throws Exception {
        when(studySessionService.getById(5L)).thenReturn(sessionResponse());

        mockMvc.perform(get("/api/sessions/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topicId").value(1));
    }

    @Test
    void returnsNotFoundForUnknownSession() throws Exception {
        when(studySessionService.getById(99L)).thenThrow(new StudySessionNotFoundException(99L));

        mockMvc.perform(get("/api/sessions/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Study session with id 99 was not found"));
    }

    @Test
    void returnsBadRequestForTaskFromAnotherTopic() throws Exception {
        when(studySessionService.create(any(Long.class), any(CreateStudySessionRequest.class)))
                .thenThrow(new LearningTaskDoesNotBelongToTopicException(10L, 1L));

        mockMvc.perform(post("/api/topics/1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateStudySessionRequest(LocalDate.now(), 60, null, Set.of(10L))
                        )))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updatesSession() throws Exception {
        when(studySessionService.update(any(Long.class), any(UpdateStudySessionRequest.class)))
                .thenReturn(sessionResponse());

        mockMvc.perform(put("/api/sessions/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateStudySessionRequest(LocalDate.of(2026, 10, 10), 60, "Updated", Set.of())
                        )))
                .andExpect(status().isOk());
    }

    @Test
    void deletesSession() throws Exception {
        doNothing().when(studySessionService).delete(5L);

        mockMvc.perform(delete("/api/sessions/5"))
                .andExpect(status().isNoContent());

        verify(studySessionService).delete(5L);
    }

    @Test
    void returnsNotFoundWhenDeletingUnknownSession() throws Exception {
        doThrow(new StudySessionNotFoundException(99L)).when(studySessionService).delete(99L);

        mockMvc.perform(delete("/api/sessions/99"))
                .andExpect(status().isNotFound());
    }

    private StudySessionResponse sessionResponse() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 10, 11, 16, 0);
        return new StudySessionResponse(
                5L, 1L, LocalDate.of(2026, 10, 11), 60, "Practice", Set.of(10L), createdAt, createdAt
        );
    }
}
