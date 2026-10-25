package com.example.codeprogress.dashboard.service;

import com.example.codeprogress.dashboard.dto.DashboardSummaryResponse;
import com.example.codeprogress.dashboard.dto.DashboardActivityResponse;
import com.example.codeprogress.dashboard.dto.DailyActivityResponse;
import com.example.codeprogress.dashboard.exception.InvalidDashboardPeriodException;
import com.example.codeprogress.dashboard.repository.DashboardRepository;
import com.example.codeprogress.dashboard.repository.DashboardStudyStatistics;
import com.example.codeprogress.dashboard.repository.DashboardTaskStatistics;
import com.example.codeprogress.learningtask.LearningTaskStatus;
import com.example.codeprogress.user.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class DashboardService {

    private static final int DASHBOARD_PERIOD_DAYS = 7;
    private static final int MAX_ACTIVITY_PERIOD_DAYS = 366;

    private final DashboardRepository dashboardRepository;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public DashboardService(
            DashboardRepository dashboardRepository,
            CurrentUserProvider currentUserProvider,
            Clock clock
    ) {
        this.dashboardRepository = dashboardRepository;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public DashboardSummaryResponse getSummary() {
        Long ownerId = currentUserProvider.getCurrentUser().getId();
        LocalDate periodEnd = LocalDate.now(clock);
        LocalDate periodStart = periodEnd.minusDays(DASHBOARD_PERIOD_DAYS - 1L);

        long totalTopics = dashboardRepository.countTopicsByOwnerId(ownerId);
        DashboardTaskStatistics taskStatistics = dashboardRepository.getTaskStatistics(
                ownerId,
                LearningTaskStatus.PLANNED,
                LearningTaskStatus.IN_PROGRESS,
                LearningTaskStatus.COMPLETED
        );
        DashboardStudyStatistics studyStatistics = dashboardRepository.getStudyStatistics(
                ownerId,
                periodStart,
                periodEnd
        );
        long totalTasks = valueOrZero(taskStatistics.getTotalTasks());
        long completedTasks = valueOrZero(taskStatistics.getCompletedTasks());
        int completionPercent = totalTasks == 0
                ? 0
                : (int) Math.round(completedTasks * 100.0 / totalTasks);

        return new DashboardSummaryResponse(
                totalTopics,
                valueOrZero(taskStatistics.getPlannedTasks()),
                valueOrZero(taskStatistics.getActiveTasks()),
                completedTasks,
                valueOrZero(studyStatistics.getTotalStudyMinutes()),
                valueOrZero(studyStatistics.getStudyMinutesLast7Days()),
                valueOrZero(studyStatistics.getStudySessionsCount()),
                completionPercent,
                studyStatistics.getLastStudyDate()
        );
    }

    @Transactional(readOnly = true)
    public DashboardActivityResponse getActivity(LocalDate from, LocalDate to) {
        validateActivityPeriod(from, to);
        Long ownerId = currentUserProvider.getCurrentUser().getId();
        List<DailyActivityResponse> days = dashboardRepository.getDailyActivity(ownerId, from, to)
                .stream()
                .map(row -> new DailyActivityResponse(
                        row.getActivityDate(),
                        valueOrZero(row.getStudyMinutes()),
                        valueOrZero(row.getStudySessionsCount())
                ))
                .toList();

        return new DashboardActivityResponse(
                from,
                to,
                clock.getZone().getId(),
                days
        );
    }

    private static void validateActivityPeriod(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new InvalidDashboardPeriodException("Both from and to dates are required");
        }
        if (from.isAfter(to)) {
            throw new InvalidDashboardPeriodException("Date from must be earlier than or equal to date to");
        }
        long periodDays = ChronoUnit.DAYS.between(from, to) + 1;
        if (periodDays > MAX_ACTIVITY_PERIOD_DAYS) {
            throw new InvalidDashboardPeriodException(
                    "Activity period must not exceed " + MAX_ACTIVITY_PERIOD_DAYS + " days"
            );
        }
    }

    private static long valueOrZero(Long value) {
        return value == null ? 0 : value;
    }
}
