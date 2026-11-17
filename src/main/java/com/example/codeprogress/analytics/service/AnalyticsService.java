package com.example.codeprogress.analytics.service;

import com.example.codeprogress.analytics.dto.AnalyticsSummaryResponse;
import com.example.codeprogress.analytics.dto.AnalyticsActivityResponse;
import com.example.codeprogress.analytics.dto.AnalyticsTopicsResponse;
import com.example.codeprogress.analytics.dto.ActivityGrouping;
import com.example.codeprogress.analytics.exception.InvalidAnalyticsPeriodException;
import com.example.codeprogress.analytics.repository.AnalyticsRepository;
import com.example.codeprogress.analytics.repository.AnalyticsStudyStatistics;
import com.example.codeprogress.analytics.repository.AnalyticsTopicRow;
import com.example.codeprogress.dashboard.repository.DashboardRepository;
import com.example.codeprogress.dashboard.repository.DashboardActivityRow;
import com.example.codeprogress.dashboard.repository.DashboardTaskStatistics;
import com.example.codeprogress.learningtask.LearningTaskStatus;
import com.example.codeprogress.user.security.CurrentUserProvider;
import com.example.codeprogress.topic.TopicStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class AnalyticsService {

    private static final int MAX_PERIOD_DAYS = 366;

    private final AnalyticsRepository analyticsRepository;
    private final DashboardRepository dashboardRepository;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public AnalyticsService(
            AnalyticsRepository analyticsRepository,
            DashboardRepository dashboardRepository,
            CurrentUserProvider currentUserProvider,
            Clock clock
    ) {
        this.analyticsRepository = analyticsRepository;
        this.dashboardRepository = dashboardRepository;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public AnalyticsSummaryResponse getSummary(LocalDate from, LocalDate to) {
        validatePeriod(from, to);
        Long ownerId = currentUserProvider.getCurrentUser().getId();
        AnalyticsStudyStatistics study = analyticsRepository.getStudyStatistics(ownerId, from, to);
        DashboardTaskStatistics tasks = dashboardRepository.getTaskStatistics(
                ownerId,
                LearningTaskStatus.PLANNED,
                LearningTaskStatus.IN_PROGRESS,
                LearningTaskStatus.COMPLETED
        );
        long total = valueOrZero(tasks.getTotalTasks());
        long completed = valueOrZero(tasks.getCompletedTasks());

        return new AnalyticsSummaryResponse(
                from,
                to,
                clock.getZone().getId(),
                new AnalyticsSummaryResponse.AllTimeStudy(
                        valueOrZero(study.getAllTimeStudyMinutes()),
                        valueOrZero(study.getAllTimeStudySessions()),
                        valueOrZero(study.getAllTimeActiveDays()),
                        study.getLastStudyDate()
                ),
                new AnalyticsSummaryResponse.PeriodStudy(
                        valueOrZero(study.getPeriodStudyMinutes()),
                        valueOrZero(study.getPeriodStudySessions()),
                        valueOrZero(study.getPeriodActiveDays())
                ),
                new AnalyticsSummaryResponse.TasksSnapshot(
                        total,
                        valueOrZero(tasks.getPlannedTasks()),
                        valueOrZero(tasks.getActiveTasks()),
                        completed,
                        total == 0 ? 0 : (int) Math.round(completed * 100.0 / total)
                )
        );
    }

    @Transactional(readOnly = true)
    public AnalyticsActivityResponse getActivity(LocalDate from, LocalDate to, ActivityGrouping groupBy) {
        validatePeriod(from, to);
        if (groupBy == null) {
            throw new InvalidAnalyticsPeriodException("Group by must be DAY or WEEK");
        }
        Long ownerId = currentUserProvider.getCurrentUser().getId();
        List<AnalyticsActivityResponse.Bucket> buckets = switch (groupBy) {
            case DAY -> dashboardRepository.getDailyActivity(ownerId, from, to).stream()
                    .map(this::dailyBucket)
                    .toList();
            case WEEK -> analyticsRepository.getWeeklyActivity(ownerId, from, to).stream()
                    .map(row -> new AnalyticsActivityResponse.Bucket(
                            row.getWeekStart().isBefore(from) ? from : row.getWeekStart(),
                            row.getWeekStart().plusDays(6).isAfter(to) ? to : row.getWeekStart().plusDays(6),
                            valueOrZero(row.getStudyMinutes()),
                            valueOrZero(row.getStudySessions())
                    ))
                    .toList();
        };
        return new AnalyticsActivityResponse(from, to, groupBy, clock.getZone().getId(), buckets);
    }

    private AnalyticsActivityResponse.Bucket dailyBucket(DashboardActivityRow row) {
        return new AnalyticsActivityResponse.Bucket(
                row.getActivityDate(), row.getActivityDate(),
                valueOrZero(row.getStudyMinutes()), valueOrZero(row.getStudySessionsCount())
        );
    }

    @Transactional(readOnly = true)
    public AnalyticsTopicsResponse getTopics(LocalDate from, LocalDate to) {
        validatePeriod(from, to);
        Long ownerId = currentUserProvider.getCurrentUser().getId();
        List<AnalyticsTopicsResponse.Topic> topics = analyticsRepository.getTopicStatistics(ownerId, from, to)
                .stream()
                .map(this::topicResponse)
                .toList();
        return new AnalyticsTopicsResponse(from, to, clock.getZone().getId(), topics);
    }

    private AnalyticsTopicsResponse.Topic topicResponse(AnalyticsTopicRow row) {
        long total = valueOrZero(row.getTotalTasks());
        long completed = valueOrZero(row.getCompletedTasks());
        return new AnalyticsTopicsResponse.Topic(
                row.getTopicId(), row.getTitle(), TopicStatus.valueOf(row.getStatus()),
                new AnalyticsTopicsResponse.Tasks(
                        total, valueOrZero(row.getPlannedTasks()), valueOrZero(row.getActiveTasks()),
                        completed, total == 0 ? 0 : (int) Math.round(completed * 100.0 / total)
                ),
                new AnalyticsTopicsResponse.PeriodStudy(
                        valueOrZero(row.getStudyMinutes()), valueOrZero(row.getStudySessions()),
                        valueOrZero(row.getActiveDays()), row.getLastStudyDate()
                )
        );
    }

    private static void validatePeriod(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new InvalidAnalyticsPeriodException("Both from and to dates are required");
        }
        if (from.isAfter(to)) {
            throw new InvalidAnalyticsPeriodException("Date from must be earlier than or equal to date to");
        }
        if (ChronoUnit.DAYS.between(from, to) >= MAX_PERIOD_DAYS) {
            throw new InvalidAnalyticsPeriodException("Analytics period must not exceed 366 days");
        }
    }

    private static long valueOrZero(Long value) {
        return value == null ? 0 : value;
    }
}
