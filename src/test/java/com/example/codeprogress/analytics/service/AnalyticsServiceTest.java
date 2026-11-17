package com.example.codeprogress.analytics.service;

import com.example.codeprogress.analytics.exception.InvalidAnalyticsPeriodException;
import com.example.codeprogress.analytics.dto.ActivityGrouping;
import com.example.codeprogress.analytics.repository.AnalyticsRepository;
import com.example.codeprogress.analytics.repository.AnalyticsStudyStatistics;
import com.example.codeprogress.analytics.repository.AnalyticsWeeklyActivity;
import com.example.codeprogress.analytics.repository.AnalyticsTopicRow;
import com.example.codeprogress.dashboard.repository.DashboardActivityRow;
import com.example.codeprogress.dashboard.repository.DashboardRepository;
import com.example.codeprogress.dashboard.repository.DashboardTaskStatistics;
import com.example.codeprogress.learningtask.LearningTaskStatus;
import com.example.codeprogress.user.User;
import com.example.codeprogress.user.security.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock private AnalyticsRepository analyticsRepository;
    @Mock private DashboardRepository dashboardRepository;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private AnalyticsStudyStatistics study;
    @Mock private DashboardTaskStatistics tasks;
    @Mock private DashboardActivityRow day;
    @Mock private AnalyticsWeeklyActivity week;
    @Mock private AnalyticsTopicRow topicRow;

    private AnalyticsService service;
    private final LocalDate from = LocalDate.of(2026, 10, 1);
    private final LocalDate to = LocalDate.of(2026, 10, 31);

    private void setUp() {
        User user = new User("owner@example.com", "test-hash");
        ReflectionTestUtils.setField(user, "id", 42L);
        when(currentUserProvider.getCurrentUser()).thenReturn(user);
        service = new AnalyticsService(
                analyticsRepository, dashboardRepository, currentUserProvider,
                Clock.system(ZoneId.of("Europe/Moscow"))
        );
    }

    @Test
    void returnsOwnerScopedSnapshotAndRoundedPercentage() {
        setUp();
        when(analyticsRepository.getStudyStatistics(42L, from, to)).thenReturn(study);
        when(dashboardRepository.getTaskStatistics(42L, LearningTaskStatus.PLANNED,
                LearningTaskStatus.IN_PROGRESS, LearningTaskStatus.COMPLETED)).thenReturn(tasks);
        when(study.getAllTimeStudyMinutes()).thenReturn(180L);
        when(study.getAllTimeStudySessions()).thenReturn(3L);
        when(study.getAllTimeActiveDays()).thenReturn(2L);
        when(study.getLastStudyDate()).thenReturn(to);
        when(study.getPeriodStudyMinutes()).thenReturn(75L);
        when(study.getPeriodStudySessions()).thenReturn(1L);
        when(study.getPeriodActiveDays()).thenReturn(1L);
        when(tasks.getTotalTasks()).thenReturn(3L);
        when(tasks.getPlannedTasks()).thenReturn(1L);
        when(tasks.getCompletedTasks()).thenReturn(2L);

        var response = service.getSummary(from, to);

        assertThat(response.timeZone()).isEqualTo("Europe/Moscow");
        assertThat(response.allTime().studyMinutes()).isEqualTo(180);
        assertThat(response.allTime().studySessions()).isEqualTo(3);
        assertThat(response.allTime().activeDays()).isEqualTo(2);
        assertThat(response.allTime().lastStudyDate()).isEqualTo(to);
        assertThat(response.period().studyMinutes()).isEqualTo(75);
        assertThat(response.period().studySessions()).isEqualTo(1);
        assertThat(response.period().activeDays()).isEqualTo(1);
        assertThat(response.tasksSnapshot().completed()).isEqualTo(2);
        assertThat(response.tasksSnapshot().completionPercent()).isEqualTo(67);
        verify(analyticsRepository).getStudyStatistics(42L, from, to);
    }

    @Test
    void returnsZeroForEmptyStatistics() {
        setUp();
        when(analyticsRepository.getStudyStatistics(42L, from, to)).thenReturn(study);
        when(dashboardRepository.getTaskStatistics(42L, LearningTaskStatus.PLANNED,
                LearningTaskStatus.IN_PROGRESS, LearningTaskStatus.COMPLETED)).thenReturn(tasks);
        var response = service.getSummary(from, to);
        assertThat(response.allTime().studyMinutes()).isZero();
        assertThat(response.allTime().lastStudyDate()).isNull();
        assertThat(response.period().activeDays()).isZero();
        assertThat(response.tasksSnapshot().completionPercent()).isZero();
    }

    @Test
    void validatesInclusiveMaximumAndRejectsInvalidPeriodsBeforeQueries() {
        setUp();
        LocalDate maxTo = from.plusDays(365);
        when(analyticsRepository.getStudyStatistics(42L, from, maxTo)).thenReturn(study);
        when(dashboardRepository.getTaskStatistics(42L, LearningTaskStatus.PLANNED,
                LearningTaskStatus.IN_PROGRESS, LearningTaskStatus.COMPLETED)).thenReturn(tasks);
        assertThat(service.getSummary(from, maxTo).to()).isEqualTo(maxTo);
        assertThatThrownBy(() -> service.getSummary(from, from.plusDays(366)))
                .isInstanceOf(InvalidAnalyticsPeriodException.class);
        assertThatThrownBy(() -> service.getSummary(to, from))
                .isInstanceOf(InvalidAnalyticsPeriodException.class);
        assertThatThrownBy(() -> service.getSummary(null, to))
                .isInstanceOf(InvalidAnalyticsPeriodException.class);
        verify(analyticsRepository, never()).getStudyStatistics(42L, from, from.plusDays(366));
    }

    @Test
    void usesRepeatableReadSnapshot() throws Exception {
        Transactional annotation = AnalyticsService.class
                .getMethod("getSummary", LocalDate.class, LocalDate.class)
                .getAnnotation(Transactional.class);
        assertThat(annotation.readOnly()).isTrue();
        assertThat(annotation.isolation()).isEqualTo(Isolation.REPEATABLE_READ);
    }

    @Test
    void mapsDailyRowsFromExistingDashboardQuery() {
        setUp();
        when(dashboardRepository.getDailyActivity(42L, from, to)).thenReturn(List.of(day));
        when(day.getActivityDate()).thenReturn(from);
        when(day.getStudyMinutes()).thenReturn(90L);
        when(day.getStudySessionsCount()).thenReturn(2L);

        var response = service.getActivity(from, to, ActivityGrouping.DAY);

        assertThat(response.timeZone()).isEqualTo("Europe/Moscow");
        assertThat(response.buckets()).hasSize(1);
        assertThat(response.buckets().getFirst().start()).isEqualTo(from);
        assertThat(response.buckets().getFirst().end()).isEqualTo(from);
        assertThat(response.buckets().getFirst().studyMinutes()).isEqualTo(90);
        assertThat(response.buckets().getFirst().studySessions()).isEqualTo(2);
        verify(dashboardRepository).getDailyActivity(42L, from, to);
    }

    @Test
    void clipsFirstAndLastIsoWeeksToRequestedRange() {
        setUp();
        LocalDate weekFrom = LocalDate.of(2026, 12, 30);
        LocalDate weekTo = LocalDate.of(2027, 1, 5);
        AnalyticsWeeklyActivity secondWeek = org.mockito.Mockito.mock(AnalyticsWeeklyActivity.class);
        when(analyticsRepository.getWeeklyActivity(42L, weekFrom, weekTo))
                .thenReturn(List.of(week, secondWeek));
        when(week.getWeekStart()).thenReturn(LocalDate.of(2026, 12, 28));
        when(week.getStudyMinutes()).thenReturn(30L);
        when(week.getStudySessions()).thenReturn(1L);
        when(secondWeek.getWeekStart()).thenReturn(LocalDate.of(2027, 1, 4));

        var buckets = service.getActivity(weekFrom, weekTo, ActivityGrouping.WEEK).buckets();

        assertThat(buckets).hasSize(2);
        assertThat(buckets.get(0).start()).isEqualTo(weekFrom);
        assertThat(buckets.get(0).end()).isEqualTo(LocalDate.of(2027, 1, 3));
        assertThat(buckets.get(0).studyMinutes()).isEqualTo(30);
        assertThat(buckets.get(1).start()).isEqualTo(LocalDate.of(2027, 1, 4));
        assertThat(buckets.get(1).end()).isEqualTo(weekTo);
        assertThat(buckets.get(1).studyMinutes()).isZero();
    }

    @Test
    void activityUsesSamePeriodValidationAsSummary() {
        setUp();
        LocalDate maxTo = from.plusDays(365);
        when(dashboardRepository.getDailyActivity(42L, from, maxTo)).thenReturn(List.of());
        assertThat(service.getActivity(from, maxTo, ActivityGrouping.DAY).buckets()).isEmpty();
        assertThatThrownBy(() -> service.getActivity(from, from.plusDays(366), ActivityGrouping.DAY))
                .isInstanceOf(InvalidAnalyticsPeriodException.class);
        assertThatThrownBy(() -> service.getActivity(to, from, ActivityGrouping.WEEK))
                .isInstanceOf(InvalidAnalyticsPeriodException.class);
        assertThatThrownBy(() -> service.getActivity(null, to, ActivityGrouping.DAY))
                .isInstanceOf(InvalidAnalyticsPeriodException.class);
        assertThatThrownBy(() -> service.getActivity(from, to, null))
                .isInstanceOf(InvalidAnalyticsPeriodException.class);
    }

    @Test
    void activityIsReadOnly() throws Exception {
        Transactional annotation = AnalyticsService.class
                .getMethod("getActivity", LocalDate.class, LocalDate.class, ActivityGrouping.class)
                .getAnnotation(Transactional.class);
        assertThat(annotation.readOnly()).isTrue();
    }

    @Test
    void mapsTopicSnapshotAndRoundsOneThird() {
        setUp();
        when(analyticsRepository.getTopicStatistics(42L, from, to)).thenReturn(List.of(topicRow));
        when(topicRow.getTopicId()).thenReturn(9L);
        when(topicRow.getTitle()).thenReturn("Java");
        when(topicRow.getStatus()).thenReturn("IN_PROGRESS");
        when(topicRow.getTotalTasks()).thenReturn(3L);
        when(topicRow.getCompletedTasks()).thenReturn(1L);
        when(topicRow.getStudyMinutes()).thenReturn(75L);

        var response = service.getTopics(from, to);

        assertThat(response.timeZone()).isEqualTo("Europe/Moscow");
        assertThat(response.topics()).hasSize(1);
        assertThat(response.topics().getFirst().tasks().completionPercent()).isEqualTo(33);
        assertThat(response.topics().getFirst().periodStudy().studyMinutes()).isEqualTo(75);
        verify(analyticsRepository).getTopicStatistics(42L, from, to);
    }

    @Test
    void mapsEmptyTopicsAndZeroAggregates() {
        setUp();
        when(analyticsRepository.getTopicStatistics(42L, from, to)).thenReturn(List.of(topicRow));
        when(topicRow.getStatus()).thenReturn("PLANNED");
        var response = service.getTopics(from, to);
        assertThat(response.topics().getFirst().tasks().completionPercent()).isZero();
        assertThat(response.topics().getFirst().periodStudy().studyMinutes()).isZero();
        assertThat(response.topics().getFirst().periodStudy().lastStudyDate()).isNull();
        when(analyticsRepository.getTopicStatistics(42L, from, to)).thenReturn(List.of());
        assertThat(service.getTopics(from, to).topics()).isEmpty();
    }

    @Test
    void topicsSharePeriodValidationAndAreReadOnly() throws Exception {
        service = new AnalyticsService(analyticsRepository, dashboardRepository, currentUserProvider,
                Clock.system(ZoneId.of("Europe/Moscow")));
        assertThatThrownBy(() -> service.getTopics(to, from)).isInstanceOf(InvalidAnalyticsPeriodException.class);
        assertThatThrownBy(() -> service.getTopics(from, from.plusDays(366)))
                .isInstanceOf(InvalidAnalyticsPeriodException.class);
        Transactional annotation = AnalyticsService.class
                .getMethod("getTopics", LocalDate.class, LocalDate.class).getAnnotation(Transactional.class);
        assertThat(annotation.readOnly()).isTrue();
    }

}
