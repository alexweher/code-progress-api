package com.example.codeprogress.analytics.controller;

import com.example.codeprogress.analytics.dto.AnalyticsSummaryResponse;
import com.example.codeprogress.analytics.dto.AnalyticsActivityResponse;
import com.example.codeprogress.analytics.dto.AnalyticsTopicsResponse;
import com.example.codeprogress.analytics.dto.ActivityGrouping;
import com.example.codeprogress.analytics.service.AnalyticsService;
import com.example.codeprogress.common.exception.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/analytics")
@Tag(name = "Learning Analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/summary")
    @Operation(
            summary = "Get the current user's learning analytics summary",
            description = "Session totals cover all time and the inclusive date range (at most 366 days). "
                    + "tasksSnapshot reflects current task statuses regardless of the date range. "
                    + "Requires a Bearer JWT."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Analytics summary returned"),
            @ApiResponse(responseCode = "400", description = "Missing, malformed or invalid date range",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Bearer token is missing or invalid",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public AnalyticsSummaryResponse getSummary(
            @Parameter(description = "Inclusive start date (YYYY-MM-DD)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Inclusive end date (YYYY-MM-DD)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return analyticsService.getSummary(from, to);
    }

    @GetMapping("/activity")
    @Operation(
            summary = "Get the current user's daily or ISO-weekly study activity",
            description = "Requires a Bearer JWT. The inclusive date range is at most 366 days. "
                    + "DAY returns each calendar day; WEEK uses Monday-Sunday ISO weeks with edge buckets "
                    + "clipped to the requested dates. Intervals without sessions have zero totals."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Activity buckets returned"),
            @ApiResponse(responseCode = "400", description = "Missing, malformed or invalid parameters",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Bearer token is missing or invalid",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public AnalyticsActivityResponse getActivity(
            @Parameter(description = "Inclusive start date (YYYY-MM-DD)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Inclusive end date (YYYY-MM-DD)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "DAY or WEEK; defaults to DAY")
            @RequestParam(defaultValue = "DAY") ActivityGrouping groupBy
    ) {
        return analyticsService.getActivity(from, to, groupBy);
    }

    @GetMapping("/topics")
    @Operation(
            summary = "Get analytics for every topic owned by the current user",
            description = "Requires a Bearer JWT. The inclusive date range is at most 366 days. "
                    + "Tasks are current status counts independent of from/to; periodStudy covers only sessions "
                    + "within the range. Topics without tasks or sessions are included with zero totals. "
                    + "Results are ordered by title, then topic ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Topic analytics returned"),
            @ApiResponse(responseCode = "400", description = "Missing, malformed or invalid date range",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Bearer token is missing or invalid",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public AnalyticsTopicsResponse getTopics(
            @Parameter(description = "Inclusive start date (YYYY-MM-DD)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Inclusive end date (YYYY-MM-DD)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return analyticsService.getTopics(from, to);
    }
}
