package com.example.codeprogress.dashboard.controller;

import com.example.codeprogress.common.exception.ApiErrorResponse;
import com.example.codeprogress.dashboard.dto.DashboardActivityResponse;
import com.example.codeprogress.dashboard.dto.DashboardSummaryResponse;
import com.example.codeprogress.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Get the current user's learning summary")
    @ApiResponse(responseCode = "200", description = "Dashboard summary returned")
    public DashboardSummaryResponse getSummary() {
        return dashboardService.getSummary();
    }

    @GetMapping("/activity")
    @Operation(
            summary = "Get daily learning activity",
            description = "Returns every date in the inclusive range, including zero-activity days. "
                    + "The maximum range is 366 days."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Daily activity returned"),
            @ApiResponse(responseCode = "400", description = "Date range is missing or invalid",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Bearer token is missing or invalid",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public DashboardActivityResponse getActivity(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return dashboardService.getActivity(from, to);
    }
}
