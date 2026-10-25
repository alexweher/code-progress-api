package com.example.codeprogress.topic.controller;

import com.example.codeprogress.common.exception.GlobalExceptionHandler;
import com.example.codeprogress.topic.TopicStatus;
import com.example.codeprogress.topic.dto.CreateTopicRequest;
import com.example.codeprogress.topic.dto.TopicResponse;
import com.example.codeprogress.topic.dto.UpdateTopicRequest;
import com.example.codeprogress.topic.dto.UpdateTopicStatusRequest;
import com.example.codeprogress.topic.exception.TopicDeletionConflictException;
import com.example.codeprogress.topic.exception.TopicNotFoundException;
import com.example.codeprogress.topic.service.TopicService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TopicController.class)
@AutoConfigureMockMvc(addFilters = false)
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
    void returnsPagedTopics() throws Exception {
        when(topicService.search(any())).thenReturn(
                new PageImpl<>(List.of(topicResponse()), PageRequest.of(1, 5), 6)
        );

        mockMvc.perform(get("/api/topics/search")
                        .param("page", "1")
                        .param("size", "5")
                        .param("sort", "title,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Java"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void rejectsPageSizeAboveMaximum() throws Exception {
        mockMvc.perform(get("/api/topics/search").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Size must be between 1 and 100"));
    }

    @Test
    void rejectsUnsupportedTopicSortField() throws Exception {
        mockMvc.perform(get("/api/topics/search").param("sort", "description,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Sorting by 'description' is not allowed"));
    }

    @Test
    void rejectsNegativePage() throws Exception {
        mockMvc.perform(get("/api/topics/search").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Page must be greater than or equal to zero"));
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
    void returnsNotFoundForUnknownApiResource() throws Exception {
        mockMvc.perform(get("/api/unknown-resource"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Resource was not found"))
                .andExpect(jsonPath("$.path").value("/api/unknown-resource"));
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
    void returnsConflictWhenDeletingTopicWithRelatedData() throws Exception {
        doThrow(new TopicDeletionConflictException(1L)).when(topicService).delete(1L);

        mockMvc.perform(delete("/api/topics/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message")
                        .value("Topic with id 1 cannot be deleted because it has learning tasks or study sessions"));
    }

    @Test
    void returnsSafeConflictForDatabaseConstraintViolation() throws Exception {
        doThrow(new DataIntegrityViolationException("duplicate key value violates constraint secret_key"))
                .when(topicService).delete(1L);

        mockMvc.perform(delete("/api/topics/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message")
                        .value("Operation conflicts with the current data state"))
                .andExpect(content().string(not(containsString("secret_key"))));
    }

    @Test
    void returnsSafeResponseForUnexpectedServerError() throws Exception {
        when(topicService.getById(1L))
                .thenThrow(new IllegalStateException("password=secret; SQL select * from topics"));

        mockMvc.perform(get("/api/topics/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("An unexpected server error occurred"))
                .andExpect(jsonPath("$.path").value("/api/topics/1"))
                .andExpect(jsonPath("$.fieldErrors").isEmpty())
                .andExpect(content().string(not(containsString("password=secret"))))
                .andExpect(content().string(not(containsString("select * from topics"))));
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
