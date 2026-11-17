package com.example.codeprogress.analytics.controller;

import com.example.codeprogress.analytics.dto.AnalyticsSummaryResponse;
import com.example.codeprogress.analytics.dto.AnalyticsActivityResponse;
import com.example.codeprogress.analytics.dto.ActivityGrouping;
import com.example.codeprogress.analytics.dto.AnalyticsTopicsResponse;
import com.example.codeprogress.topic.TopicStatus;
import com.example.codeprogress.analytics.exception.InvalidAnalyticsPeriodException;
import com.example.codeprogress.analytics.service.AnalyticsService;
import com.example.codeprogress.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnalyticsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AnalyticsControllerTest {

    @MockitoBean private AnalyticsService analyticsService;
    @Autowired private MockMvc mockMvc;

    @Test
    void returnsSummaryJson() throws Exception {
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 31);
        when(analyticsService.getSummary(from, to)).thenReturn(new AnalyticsSummaryResponse(
                from, to, "Europe/Moscow",
                new AnalyticsSummaryResponse.AllTimeStudy(120, 2, 2, to),
                new AnalyticsSummaryResponse.PeriodStudy(45, 1, 1),
                new AnalyticsSummaryResponse.TasksSnapshot(3, 1, 0, 2, 67)
        ));
        mockMvc.perform(get("/api/analytics/summary")
                        .param("from", from.toString()).param("to", to.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timeZone").value("Europe/Moscow"))
                .andExpect(jsonPath("$.allTime.studyMinutes").value(120))
                .andExpect(jsonPath("$.period.studyMinutes").value(45))
                .andExpect(jsonPath("$.tasksSnapshot.completionPercent").value(67));
    }

    @Test
    void returnsExistingBadRequestFormat() throws Exception {
        mockMvc.perform(get("/api/analytics/summary").param("from", "bad").param("to", "2026-10-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'from'"));
        mockMvc.perform(get("/api/analytics/summary").param("from", "2026-10-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Required parameter 'to' is missing"));
        LocalDate from = LocalDate.of(2026, 10, 31);
        LocalDate to = LocalDate.of(2026, 10, 1);
        when(analyticsService.getSummary(from, to)).thenThrow(new InvalidAnalyticsPeriodException("Invalid period"));
        mockMvc.perform(get("/api/analytics/summary")
                        .param("from", from.toString()).param("to", to.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid period"));
    }

    @Test
    void defaultsToDayAndReturnsActivityBuckets() throws Exception {
        LocalDate date = LocalDate.of(2026, 10, 1);
        when(analyticsService.getActivity(date, date, ActivityGrouping.DAY))
                .thenReturn(new AnalyticsActivityResponse(date, date, ActivityGrouping.DAY, "UTC",
                        List.of(new AnalyticsActivityResponse.Bucket(date, date, 45, 1))));
        mockMvc.perform(get("/api/analytics/activity")
                        .param("from", date.toString()).param("to", date.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupBy").value("DAY"))
                .andExpect(jsonPath("$.timeZone").value("UTC"))
                .andExpect(jsonPath("$.buckets[0].studyMinutes").value(45));
    }

    @Test
    void acceptsWeekAndRejectsBadParameters() throws Exception {
        LocalDate date = LocalDate.of(2026, 10, 1);
        when(analyticsService.getActivity(date, date, ActivityGrouping.WEEK))
                .thenReturn(new AnalyticsActivityResponse(date, date, ActivityGrouping.WEEK, "UTC", List.of()));
        mockMvc.perform(get("/api/analytics/activity")
                        .param("from", date.toString()).param("to", date.toString())
                        .param("groupBy", "WEEK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupBy").value("WEEK"));
        mockMvc.perform(get("/api/analytics/activity")
                        .param("from", date.toString()).param("to", date.toString())
                        .param("groupBy", "MONTH"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        mockMvc.perform(get("/api/analytics/activity")
                        .param("from", "invalid").param("to", date.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'from'"));
        mockMvc.perform(get("/api/analytics/activity").param("from", date.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Required parameter 'to' is missing"));
    }

    @Test
    void returnsTopicsResponseAndExistingValidationErrors() throws Exception {
        LocalDate date = LocalDate.of(2026, 10, 1);
        when(analyticsService.getTopics(date, date)).thenReturn(new AnalyticsTopicsResponse(
                date, date, "UTC", List.of(new AnalyticsTopicsResponse.Topic(
                4L, "Java", TopicStatus.IN_PROGRESS,
                new AnalyticsTopicsResponse.Tasks(3, 1, 0, 2, 67),
                new AnalyticsTopicsResponse.PeriodStudy(45, 1, 1, date)
        ))));
        mockMvc.perform(get("/api/analytics/topics")
                        .param("from", date.toString()).param("to", date.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timeZone").value("UTC"))
                .andExpect(jsonPath("$.topics[0].tasks.completionPercent").value(67))
                .andExpect(jsonPath("$.topics[0].periodStudy.studyMinutes").value(45));
        mockMvc.perform(get("/api/analytics/topics").param("from", "bad").param("to", date.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        mockMvc.perform(get("/api/analytics/topics").param("from", date.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Required parameter 'to' is missing"));
    }
}
