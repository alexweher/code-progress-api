package com.example.codeprogress.topic.service;

import com.example.codeprogress.learningtask.repository.LearningTaskRepository;
import com.example.codeprogress.studysession.repository.StudySessionRepository;
import com.example.codeprogress.studysession.repository.StudySessionStatistics;
import com.example.codeprogress.topic.dto.TopicProgressResponse;
import com.example.codeprogress.topic.exception.TopicNotFoundException;
import com.example.codeprogress.topic.repository.TopicRepository;
import com.example.codeprogress.user.User;
import com.example.codeprogress.user.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TopicProgressServiceTest {

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private LearningTaskRepository learningTaskRepository;

    @Mock
    private StudySessionRepository studySessionRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    private TopicProgressService topicProgressService;

    @BeforeEach
    void setUp() {
        User currentUser = new User("owner@example.com", "password-hash");
        ReflectionTestUtils.setField(currentUser, "id", 100L);
        lenient().when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
        topicProgressService = new TopicProgressService(
                topicRepository,
                learningTaskRepository,
                studySessionRepository,
                currentUserProvider
        );
    }

    @Test
    void calculatesProgressFromTasksAndSessions() {
        when(topicRepository.existsByIdAndOwnerId(1L, 100L)).thenReturn(true);
        when(learningTaskRepository.countByTopicIdAndTopicOwnerId(1L, 100L)).thenReturn(3L);
        when(learningTaskRepository.countByTopicIdAndTopicOwnerIdAndStatus(any(), any(), any()))
                .thenReturn(2L);
        when(studySessionRepository.getStatisticsByTopicIdAndOwnerId(1L, 100L))
                .thenReturn(statistics(120L, 2L, LocalDate.of(2026, 10, 11)));

        TopicProgressResponse response = topicProgressService.getProgress(1L);

        assertThat(response.totalTasks()).isEqualTo(3);
        assertThat(response.completedTasks()).isEqualTo(2);
        assertThat(response.progressPercent()).isEqualTo(67);
        assertThat(response.totalStudyMinutes()).isEqualTo(120);
        assertThat(response.studySessionsCount()).isEqualTo(2);
        assertThat(response.lastStudyDate()).isEqualTo(LocalDate.of(2026, 10, 11));
    }

    @Test
    void returnsZeroProgressForTopicWithoutTasksOrSessions() {
        when(topicRepository.existsByIdAndOwnerId(1L, 100L)).thenReturn(true);
        when(learningTaskRepository.countByTopicIdAndTopicOwnerId(1L, 100L)).thenReturn(0L);
        when(learningTaskRepository.countByTopicIdAndTopicOwnerIdAndStatus(any(), any(), any()))
                .thenReturn(0L);
        when(studySessionRepository.getStatisticsByTopicIdAndOwnerId(1L, 100L))
                .thenReturn(statistics(0L, 0L, null));

        TopicProgressResponse response = topicProgressService.getProgress(1L);

        assertThat(response.progressPercent()).isZero();
        assertThat(response.totalStudyMinutes()).isZero();
        assertThat(response.lastStudyDate()).isNull();
    }

    @Test
    void throwsForUnknownTopic() {
        when(topicRepository.existsByIdAndOwnerId(99L, 100L)).thenReturn(false);

        assertThatThrownBy(() -> topicProgressService.getProgress(99L))
                .isInstanceOf(TopicNotFoundException.class);
    }

    @Test
    void doesNotCalculateProgressForAnotherUsersTopic() {
        User anotherUser = new User("another@example.com", "password-hash");
        ReflectionTestUtils.setField(anotherUser, "id", 200L);
        when(currentUserProvider.getCurrentUser()).thenReturn(anotherUser);
        when(topicRepository.existsByIdAndOwnerId(1L, 200L)).thenReturn(false);

        assertThatThrownBy(() -> topicProgressService.getProgress(1L))
                .isInstanceOf(TopicNotFoundException.class);

        verify(learningTaskRepository, never()).countByTopicIdAndTopicOwnerId(any(), any());
        verify(studySessionRepository, never()).getStatisticsByTopicIdAndOwnerId(any(), any());
    }

    @Test
    void usesRepeatableReadSnapshotForProgressCalculation() throws NoSuchMethodException {
        Transactional transactional = TopicProgressService.class
                .getMethod("getProgress", Long.class)
                .getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.readOnly()).isTrue();
        assertThat(transactional.isolation()).isEqualTo(Isolation.REPEATABLE_READ);
    }

    private StudySessionStatistics statistics(long totalMinutes, long sessionsCount, LocalDate lastStudyDate) {
        return new StudySessionStatistics() {
            @Override
            public Long getTotalStudyMinutes() {
                return totalMinutes;
            }

            @Override
            public Long getStudySessionsCount() {
                return sessionsCount;
            }

            @Override
            public LocalDate getLastStudyDate() {
                return lastStudyDate;
            }
        };
    }
}
