package com.example.codeprogress.dashboard.controller;

import com.example.codeprogress.common.exception.GlobalExceptionHandler;
import com.example.codeprogress.dashboard.dto.DailyActivityResponse;
import com.example.codeprogress.dashboard.dto.DashboardActivityResponse;
import com.example.codeprogress.dashboard.dto.DashboardSummaryResponse;
import com.example.codeprogress.dashboard.exception.InvalidDashboardPeriodException;
import com.example.codeprogress.dashboard.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class DashboardControllerTest {

    @MockitoBean
    private DashboardService dashboardService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsDashboardSummary() throws Exception {
        when(dashboardService.getSummary()).thenReturn(new DashboardSummaryResponse(
                4L,
                2L,
                1L,
                3L,
                420L,
                150L,
                8L,
                50,
                LocalDate.of(2026, 10, 20)
        ));

        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTopics").value(4))
                .andExpect(jsonPath("$.plannedTasks").value(2))
                .andExpect(jsonPath("$.activeTasks").value(1))
                .andExpect(jsonPath("$.completedTasks").value(3))
                .andExpect(jsonPath("$.totalStudyMinutes").value(420))
                .andExpect(jsonPath("$.studyMinutesLast7Days").value(150))
                .andExpect(jsonPath("$.studySessionsCount").value(8))
                .andExpect(jsonPath("$.completionPercent").value(50))
                .andExpect(jsonPath("$.lastStudyDate").value("2026-10-20"));
    }

    @Test
    void returnsActivityForChart() throws Exception {
        LocalDate from = LocalDate.of(2026, 10, 18);
        LocalDate to = LocalDate.of(2026, 10, 20);
        when(dashboardService.getActivity(from, to)).thenReturn(new DashboardActivityResponse(
                from,
                to,
                "Europe/Moscow",
                List.of(
                        new DailyActivityResponse(from, 0L, 0L),
                        new DailyActivityResponse(LocalDate.of(2026, 10, 19), 90L, 2L),
                        new DailyActivityResponse(to, 30L, 1L)
                )
        ));

        mockMvc.perform(get("/api/dashboard/activity")
                        .param("from", "2026-10-18")
                        .param("to", "2026-10-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2026-10-18"))
                .andExpect(jsonPath("$.to").value("2026-10-20"))
                .andExpect(jsonPath("$.timeZone").value("Europe/Moscow"))
                .andExpect(jsonPath("$.days.length()").value(3))
                .andExpect(jsonPath("$.days[0].studyMinutes").value(0))
                .andExpect(jsonPath("$.days[1].studyMinutes").value(90))
                .andExpect(jsonPath("$.days[1].studySessionsCount").value(2));
    }

    @Test
    void returnsBadRequestForInvalidPeriod() throws Exception {
        LocalDate from = LocalDate.of(2026, 10, 20);
        LocalDate to = LocalDate.of(2026, 10, 19);
        when(dashboardService.getActivity(from, to)).thenThrow(
                new InvalidDashboardPeriodException("Date from must be earlier than or equal to date to")
        );

        mockMvc.perform(get("/api/dashboard/activity")
                        .param("from", "2026-10-20")
                        .param("to", "2026-10-19"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Date from must be earlier than or equal to date to"));
    }

    @Test
    void returnsBadRequestForMalformedDate() throws Exception {
        mockMvc.perform(get("/api/dashboard/activity")
                        .param("from", "20-10-2026")
                        .param("to", "2026-10-20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'from'"));
    }

    @Test
    void returnsBadRequestWhenPeriodBoundaryIsMissing() throws Exception {
        mockMvc.perform(get("/api/dashboard/activity")
                        .param("from", "2026-10-20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Required parameter 'to' is missing"));
    }
}
