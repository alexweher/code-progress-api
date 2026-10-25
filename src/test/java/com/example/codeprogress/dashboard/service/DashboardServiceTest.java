package com.example.codeprogress.dashboard.service;

import com.example.codeprogress.dashboard.dto.DashboardActivityResponse;
import com.example.codeprogress.dashboard.dto.DashboardSummaryResponse;
import com.example.codeprogress.dashboard.exception.InvalidDashboardPeriodException;
import com.example.codeprogress.dashboard.repository.DashboardActivityRow;
import com.example.codeprogress.dashboard.repository.DashboardRepository;
import com.example.codeprogress.dashboard.repository.DashboardStudyStatistics;
import com.example.codeprogress.dashboard.repository.DashboardTaskStatistics;
import com.example.codeprogress.learningtask.LearningTaskStatus;
import com.example.codeprogress.user.User;
import com.example.codeprogress.user.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-10-19T22:30:00Z"),
            ZoneId.of("Europe/Moscow")
    );

    @Mock
    private DashboardRepository dashboardRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        User currentUser = user(100L, "owner@example.com");
        lenient().when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
        dashboardService = new DashboardService(dashboardRepository, currentUserProvider, CLOCK);
    }

    @Test
    void calculatesSummaryUsingOwnerAndInclusiveSevenDayPeriod() {
        when(dashboardRepository.countTopicsByOwnerId(100L)).thenReturn(4L);
        when(dashboardRepository.getTaskStatistics(
                100L,
                LearningTaskStatus.PLANNED,
                LearningTaskStatus.IN_PROGRESS,
                LearningTaskStatus.COMPLETED
        )).thenReturn(taskStatistics(6L, 2L, 1L, 3L));
        when(dashboardRepository.getStudyStatistics(
                100L,
                LocalDate.of(2026, 10, 14),
                LocalDate.of(2026, 10, 20)
        )).thenReturn(studyStatistics(420L, 150L, 8L, LocalDate.of(2026, 10, 20)));

        DashboardSummaryResponse response = dashboardService.getSummary();

        assertThat(response.totalTopics()).isEqualTo(4);
        assertThat(response.plannedTasks()).isEqualTo(2);
        assertThat(response.activeTasks()).isEqualTo(1);
        assertThat(response.completedTasks()).isEqualTo(3);
        assertThat(response.totalStudyMinutes()).isEqualTo(420);
        assertThat(response.studyMinutesLast7Days()).isEqualTo(150);
        assertThat(response.studySessionsCount()).isEqualTo(8);
        assertThat(response.completionPercent()).isEqualTo(50);
        assertThat(response.lastStudyDate()).isEqualTo(LocalDate.of(2026, 10, 20));
    }

    @Test
    void returnsZerosForUserWithoutLearningData() {
        when(dashboardRepository.countTopicsByOwnerId(100L)).thenReturn(0L);
        when(dashboardRepository.getTaskStatistics(
                100L,
                LearningTaskStatus.PLANNED,
                LearningTaskStatus.IN_PROGRESS,
                LearningTaskStatus.COMPLETED
        )).thenReturn(taskStatistics(0L, 0L, 0L, 0L));
        when(dashboardRepository.getStudyStatistics(
                100L,
                LocalDate.of(2026, 10, 14),
                LocalDate.of(2026, 10, 20)
        )).thenReturn(studyStatistics(0L, 0L, 0L, null));

        DashboardSummaryResponse response = dashboardService.getSummary();

        assertThat(response.totalTopics()).isZero();
        assertThat(response.plannedTasks()).isZero();
        assertThat(response.activeTasks()).isZero();
        assertThat(response.completedTasks()).isZero();
        assertThat(response.totalStudyMinutes()).isZero();
        assertThat(response.studyMinutesLast7Days()).isZero();
        assertThat(response.studySessionsCount()).isZero();
        assertThat(response.completionPercent()).isZero();
        assertThat(response.lastStudyDate()).isNull();
    }

    @Test
    void returnsZeroPercentWhenUserHasSessionsButNoTasks() {
        when(dashboardRepository.countTopicsByOwnerId(100L)).thenReturn(1L);
        when(dashboardRepository.getTaskStatistics(
                100L,
                LearningTaskStatus.PLANNED,
                LearningTaskStatus.IN_PROGRESS,
                LearningTaskStatus.COMPLETED
        )).thenReturn(taskStatistics(0L, 0L, 0L, 0L));
        when(dashboardRepository.getStudyStatistics(
                100L,
                LocalDate.of(2026, 10, 14),
                LocalDate.of(2026, 10, 20)
        )).thenReturn(studyStatistics(45L, 45L, 1L, LocalDate.of(2026, 10, 18)));

        DashboardSummaryResponse response = dashboardService.getSummary();

        assertThat(response.completionPercent()).isZero();
        assertThat(response.totalStudyMinutes()).isEqualTo(45);
        assertThat(response.studySessionsCount()).isEqualTo(1);
    }

    @Test
    void roundsCompletionPercentToNearestWholeNumber() {
        when(dashboardRepository.countTopicsByOwnerId(100L)).thenReturn(1L);
        when(dashboardRepository.getTaskStatistics(
                100L,
                LearningTaskStatus.PLANNED,
                LearningTaskStatus.IN_PROGRESS,
                LearningTaskStatus.COMPLETED
        )).thenReturn(taskStatistics(3L, 1L, 0L, 2L));
        when(dashboardRepository.getStudyStatistics(
                100L,
                LocalDate.of(2026, 10, 14),
                LocalDate.of(2026, 10, 20)
        )).thenReturn(studyStatistics(0L, 0L, 0L, null));

        assertThat(dashboardService.getSummary().completionPercent()).isEqualTo(67);
    }

    @Test
    void isolatesAggregatesByCurrentUser() {
        User anotherUser = user(200L, "another@example.com");
        when(currentUserProvider.getCurrentUser()).thenReturn(anotherUser);
        when(dashboardRepository.countTopicsByOwnerId(200L)).thenReturn(1L);
        when(dashboardRepository.getTaskStatistics(
                200L,
                LearningTaskStatus.PLANNED,
                LearningTaskStatus.IN_PROGRESS,
                LearningTaskStatus.COMPLETED
        )).thenReturn(taskStatistics(1L, 1L, 0L, 0L));
        when(dashboardRepository.getStudyStatistics(
                200L,
                LocalDate.of(2026, 10, 14),
                LocalDate.of(2026, 10, 20)
        )).thenReturn(studyStatistics(15L, 15L, 1L, LocalDate.of(2026, 10, 19)));

        DashboardSummaryResponse response = dashboardService.getSummary();

        assertThat(response.totalTopics()).isEqualTo(1);
        verify(dashboardRepository).countTopicsByOwnerId(200L);
        verify(dashboardRepository).getStudyStatistics(
                200L,
                LocalDate.of(2026, 10, 14),
                LocalDate.of(2026, 10, 20)
        );
    }

    @Test
    void usesRepeatableReadSnapshot() throws NoSuchMethodException {
        Transactional transactional = DashboardService.class
                .getMethod("getSummary")
                .getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.readOnly()).isTrue();
        assertThat(transactional.isolation()).isEqualTo(Isolation.REPEATABLE_READ);
    }

    @Test
    void returnsZeroRowsForDaysWithoutSessions() {
        LocalDate from = LocalDate.of(2026, 10, 18);
        LocalDate to = LocalDate.of(2026, 10, 20);
        when(dashboardRepository.getDailyActivity(100L, from, to)).thenReturn(List.of(
                activityRow(from, 0L, 0L),
                activityRow(LocalDate.of(2026, 10, 19), 0L, 0L),
                activityRow(to, 0L, 0L)
        ));

        DashboardActivityResponse response = dashboardService.getActivity(from, to);

        assertThat(response.days()).hasSize(3);
        assertThat(response.days()).allSatisfy(day -> {
            assertThat(day.studyMinutes()).isZero();
            assertThat(day.studySessionsCount()).isZero();
        });
        assertThat(response.timeZone()).isEqualTo("Europe/Moscow");
    }

    @Test
    void returnsAggregatedMultipleSessionsForOneDay() {
        LocalDate date = LocalDate.of(2026, 10, 20);
        when(dashboardRepository.getDailyActivity(100L, date, date))
                .thenReturn(List.of(activityRow(date, 105L, 3L)));

        DashboardActivityResponse response = dashboardService.getActivity(date, date);

        assertThat(response.days()).singleElement().satisfies(day -> {
            assertThat(day.date()).isEqualTo(date);
            assertThat(day.studyMinutes()).isEqualTo(105);
            assertThat(day.studySessionsCount()).isEqualTo(3);
        });
    }

    @Test
    void acceptsInclusiveMaximumPeriodAndPreservesBoundaryDates() {
        LocalDate from = LocalDate.of(2025, 10, 20);
        LocalDate to = LocalDate.of(2026, 10, 20);
        when(dashboardRepository.getDailyActivity(100L, from, to)).thenReturn(List.of(
                activityRow(from, 15L, 1L),
                activityRow(to, 30L, 1L)
        ));

        DashboardActivityResponse response = dashboardService.getActivity(from, to);

        assertThat(response.from()).isEqualTo(from);
        assertThat(response.to()).isEqualTo(to);
        verify(dashboardRepository).getDailyActivity(100L, from, to);
    }

    @Test
    void rejectsReversedAndTooLongPeriodsBeforeQueryingDatabase() {
        assertThatThrownBy(() -> dashboardService.getActivity(
                LocalDate.of(2026, 10, 20),
                LocalDate.of(2026, 10, 19)
        )).isInstanceOf(InvalidDashboardPeriodException.class)
                .hasMessage("Date from must be earlier than or equal to date to");

        assertThatThrownBy(() -> dashboardService.getActivity(
                LocalDate.of(2025, 10, 19),
                LocalDate.of(2026, 10, 20)
        )).isInstanceOf(InvalidDashboardPeriodException.class)
                .hasMessage("Activity period must not exceed 366 days");

        verify(dashboardRepository, never()).getDailyActivity(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void isolatesActivityByCurrentUser() {
        User anotherUser = user(200L, "another@example.com");
        when(currentUserProvider.getCurrentUser()).thenReturn(anotherUser);
        LocalDate date = LocalDate.of(2026, 10, 20);
        when(dashboardRepository.getDailyActivity(200L, date, date))
                .thenReturn(List.of(activityRow(date, 25L, 1L)));

        DashboardActivityResponse response = dashboardService.getActivity(date, date);

        assertThat(response.days()).singleElement()
                .satisfies(day -> assertThat(day.studyMinutes()).isEqualTo(25));
        verify(dashboardRepository).getDailyActivity(200L, date, date);
    }

    private static User user(Long id, String email) {
        User user = new User(email, "password-hash");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static DashboardTaskStatistics taskStatistics(
            long total,
            long planned,
            long active,
            long completed
    ) {
        return new DashboardTaskStatistics() {
            public Long getTotalTasks() { return total; }
            public Long getPlannedTasks() { return planned; }
            public Long getActiveTasks() { return active; }
            public Long getCompletedTasks() { return completed; }
        };
    }

    private static DashboardStudyStatistics studyStatistics(
            long totalMinutes,
            long recentMinutes,
            long sessions,
            LocalDate lastStudyDate
    ) {
        return new DashboardStudyStatistics() {
            public Long getTotalStudyMinutes() { return totalMinutes; }
            public Long getStudyMinutesLast7Days() { return recentMinutes; }
            public Long getStudySessionsCount() { return sessions; }
            public LocalDate getLastStudyDate() { return lastStudyDate; }
        };
    }

    private static DashboardActivityRow activityRow(LocalDate date, long minutes, long sessions) {
        return new DashboardActivityRow() {
            public LocalDate getActivityDate() { return date; }
            public Long getStudyMinutes() { return minutes; }
            public Long getStudySessionsCount() { return sessions; }
        };
    }
}
