package com.example.codeprogress.studysession.service;

import com.example.codeprogress.learningtask.LearningTask;
import com.example.codeprogress.learningtask.LearningTaskDifficulty;
import com.example.codeprogress.learningtask.LearningTaskStatus;
import com.example.codeprogress.learningtask.exception.LearningTaskNotFoundException;
import com.example.codeprogress.learningtask.repository.LearningTaskRepository;
import com.example.codeprogress.studysession.StudySession;
import com.example.codeprogress.studysession.dto.CreateStudySessionRequest;
import com.example.codeprogress.studysession.dto.StudySessionResponse;
import com.example.codeprogress.studysession.dto.UpdateStudySessionRequest;
import com.example.codeprogress.studysession.exception.LearningTaskDoesNotBelongToTopicException;
import com.example.codeprogress.studysession.exception.StudySessionNotFoundException;
import com.example.codeprogress.studysession.repository.StudySessionRepository;
import com.example.codeprogress.topic.Topic;
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
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudySessionServiceTest {

    @Mock
    private StudySessionRepository studySessionRepository;

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private LearningTaskRepository learningTaskRepository;

    private StudySessionService studySessionService;

    @BeforeEach
    void setUp() {
        studySessionService = new StudySessionService(studySessionRepository, topicRepository, learningTaskRepository);
    }

    @Test
    void createsSessionAndCompletesItsTasks() {
        Topic topic = topic(1L, "Java");
        LearningTask task = task(10L, topic, LearningTaskStatus.PLANNED);
        when(topicRepository.findById(1L)).thenReturn(Optional.of(topic));
        when(learningTaskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(studySessionRepository.save(any(StudySession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudySessionResponse response = studySessionService.create(
                1L,
                new CreateStudySessionRequest(LocalDate.of(2026, 10, 11), 90, "REST API", Set.of(10L))
        );

        assertThat(response.topicId()).isEqualTo(1L);
        assertThat(response.completedTaskIds()).containsExactly(10L);
        assertThat(task.getStatus()).isEqualTo(LearningTaskStatus.COMPLETED);
    }

    @Test
    void allowsSessionWithoutCompletedTasks() {
        Topic topic = topic(1L, "Java");
        when(topicRepository.findById(1L)).thenReturn(Optional.of(topic));
        when(studySessionRepository.save(any(StudySession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudySessionResponse response = studySessionService.create(
                1L,
                new CreateStudySessionRequest(LocalDate.of(2026, 10, 11), 30, null, Set.of())
        );

        assertThat(response.completedTaskIds()).isEmpty();
    }

    @Test
    void throwsWhenCreatingSessionForUnknownTopic() {
        when(topicRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studySessionService.create(
                99L, new CreateStudySessionRequest(LocalDate.now(), 30, null, Set.of())
        )).isInstanceOf(TopicNotFoundException.class);
    }

    @Test
    void throwsWhenCompletedTaskIsUnknown() {
        Topic topic = topic(1L, "Java");
        when(topicRepository.findById(1L)).thenReturn(Optional.of(topic));
        when(learningTaskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studySessionService.create(
                1L, new CreateStudySessionRequest(LocalDate.now(), 30, null, Set.of(99L))
        )).isInstanceOf(LearningTaskNotFoundException.class);
    }

    @Test
    void rejectsTaskFromAnotherTopic() {
        Topic java = topic(1L, "Java");
        Topic sql = topic(2L, "SQL");
        LearningTask sqlTask = task(10L, sql, LearningTaskStatus.PLANNED);
        when(topicRepository.findById(1L)).thenReturn(Optional.of(java));
        when(learningTaskRepository.findById(10L)).thenReturn(Optional.of(sqlTask));

        assertThatThrownBy(() -> studySessionService.create(
                1L, new CreateStudySessionRequest(LocalDate.now(), 30, null, Set.of(10L))
        )).isInstanceOf(LearningTaskDoesNotBelongToTopicException.class);
    }

    @Test
    void returnsSessionsForTopic() {
        Topic topic = topic(1L, "Java");
        when(topicRepository.findById(1L)).thenReturn(Optional.of(topic));
        when(studySessionRepository.findAllByTopicIdOrderBySessionDateDesc(1L)).thenReturn(List.of(
                session(5L, topic, Set.of())
        ));

        List<StudySessionResponse> responses = studySessionService.getAllByTopicId(1L);

        assertThat(responses).extracting(StudySessionResponse::id).containsExactly(5L);
    }

    @Test
    void updatesSessionAndCompletesNewTask() {
        Topic topic = topic(1L, "Java");
        StudySession session = session(5L, topic, Set.of());
        LearningTask task = task(10L, topic, LearningTaskStatus.PLANNED);
        when(studySessionRepository.findById(5L)).thenReturn(Optional.of(session));
        when(learningTaskRepository.findById(10L)).thenReturn(Optional.of(task));

        StudySessionResponse response = studySessionService.update(
                5L,
                new UpdateStudySessionRequest(LocalDate.of(2026, 10, 10), 120, "Updated", Set.of(10L))
        );

        assertThat(response.durationMinutes()).isEqualTo(120);
        assertThat(task.getStatus()).isEqualTo(LearningTaskStatus.COMPLETED);
    }

    @Test
    void throwsWhenGettingUnknownSession() {
        when(studySessionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studySessionService.getById(99L))
                .isInstanceOf(StudySessionNotFoundException.class);
    }

    @Test
    void deletesSession() {
        Topic topic = topic(1L, "Java");
        StudySession session = session(5L, topic, Set.of());
        when(studySessionRepository.findById(5L)).thenReturn(Optional.of(session));

        studySessionService.delete(5L);

        verify(studySessionRepository).delete(session);
    }

    private Topic topic(Long id, String title) {
        Topic topic = new Topic(title, null);
        ReflectionTestUtils.setField(topic, "id", id);
        return topic;
    }

    private LearningTask task(Long id, Topic topic, LearningTaskStatus status) {
        LearningTask task = new LearningTask(topic, "Task", null, LearningTaskDifficulty.EASY);
        if (status != LearningTaskStatus.PLANNED) {
            task.updateStatus(status);
        }
        ReflectionTestUtils.setField(task, "id", id);
        return task;
    }

    private StudySession session(Long id, Topic topic, Set<LearningTask> tasks) {
        StudySession session = new StudySession(topic, LocalDate.of(2026, 10, 11), 60, "Note", tasks);
        ReflectionTestUtils.setField(session, "id", id);
        return session;
    }
}
