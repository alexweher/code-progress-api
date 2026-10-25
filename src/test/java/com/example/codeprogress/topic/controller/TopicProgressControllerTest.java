package com.example.codeprogress.topic.controller;

import com.example.codeprogress.common.exception.GlobalExceptionHandler;
import com.example.codeprogress.topic.dto.TopicProgressResponse;
import com.example.codeprogress.topic.exception.TopicNotFoundException;
import com.example.codeprogress.topic.service.TopicProgressService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TopicProgressController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class TopicProgressControllerTest {

    @MockitoBean
    private TopicProgressService topicProgressService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsProgress() throws Exception {
        when(topicProgressService.getProgress(1L)).thenReturn(new TopicProgressResponse(
                1L, 3L, 2L, 67, 120L, 2L, LocalDate.of(2026, 10, 11)
        ));

        mockMvc.perform(get("/api/topics/1/progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progressPercent").value(67))
                .andExpect(jsonPath("$.totalStudyMinutes").value(120));
    }

    @Test
    void returnsNotFoundForUnknownTopic() throws Exception {
        when(topicProgressService.getProgress(99L)).thenThrow(new TopicNotFoundException(99L));

        mockMvc.perform(get("/api/topics/99/progress"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Topic with id 99 was not found"));
    }
}
