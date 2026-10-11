package com.example.codeprogress.topic.service;

import com.example.codeprogress.learningtask.repository.LearningTaskRepository;
import com.example.codeprogress.studysession.StudySession;
import com.example.codeprogress.studysession.repository.StudySessionRepository;
import com.example.codeprogress.topic.Topic;
import com.example.codeprogress.topic.dto.TopicProgressResponse;
import com.example.codeprogress.topic.exception.TopicNotFoundException;
import com.example.codeprogress.topic.repository.TopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TopicProgressServiceTest {

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private LearningTaskRepository learningTaskRepository;

    @Mock
    private StudySessionRepository studySessionRepository;

    private TopicProgressService topicProgressService;

    @BeforeEach
    void setUp() {
        topicProgressService = new TopicProgressService(
                topicRepository, learningTaskRepository, studySessionRepository
        );
    }

    @Test
    void calculatesProgressFromTasksAndSessions() {
        when(topicRepository.existsById(1L)).thenReturn(true);
        when(learningTaskRepository.countByTopicId(1L)).thenReturn(3L);
        when(learningTaskRepository.countByTopicIdAndStatus(any(), any())).thenReturn(2L);
        when(studySessionRepository.findAllByTopicIdOrderBySessionDateDesc(1L)).thenReturn(List.of(
                session(1L, LocalDate.of(2026, 10, 10), 45),
                session(2L, LocalDate.of(2026, 10, 11), 75)
        ));

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
        when(topicRepository.existsById(1L)).thenReturn(true);
        when(learningTaskRepository.countByTopicId(1L)).thenReturn(0L);
        when(learningTaskRepository.countByTopicIdAndStatus(any(), any())).thenReturn(0L);
        when(studySessionRepository.findAllByTopicIdOrderBySessionDateDesc(1L)).thenReturn(List.of());

        TopicProgressResponse response = topicProgressService.getProgress(1L);

        assertThat(response.progressPercent()).isZero();
        assertThat(response.totalStudyMinutes()).isZero();
        assertThat(response.lastStudyDate()).isNull();
    }

    @Test
    void throwsForUnknownTopic() {
        when(topicRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> topicProgressService.getProgress(99L))
                .isInstanceOf(TopicNotFoundException.class);
    }

    private StudySession session(Long id, LocalDate date, int durationMinutes) {
        Topic topic = new Topic("Java", null);
        ReflectionTestUtils.setField(topic, "id", 1L);
        StudySession session = new StudySession(topic, date, durationMinutes, null, Set.of());
        ReflectionTestUtils.setField(session, "id", id);
        return session;
    }
}
