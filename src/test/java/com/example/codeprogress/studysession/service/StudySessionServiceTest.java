package com.example.codeprogress.studysession.service;

import com.example.codeprogress.learningtask.LearningTask;
import com.example.codeprogress.common.pagination.InvalidPageRequestException;
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
import com.example.codeprogress.user.User;
import com.example.codeprogress.user.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
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

    @Mock
    private CurrentUserProvider currentUserProvider;

    private StudySessionService studySessionService;
    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = new User("owner@example.com", "password-hash");
        ReflectionTestUtils.setField(currentUser, "id", 100L);
        lenient().when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
        studySessionService = new StudySessionService(
                studySessionRepository,
                topicRepository,
                learningTaskRepository,
                currentUserProvider
        );
    }

    @Test
    void createsSessionAndCompletesItsTasks() {
        Topic topic = topic(1L, "Java");
        LearningTask task = task(10L, topic, LearningTaskStatus.PLANNED);
        when(topicRepository.findByIdAndOwnerId(1L, 100L)).thenReturn(Optional.of(topic));
        when(learningTaskRepository.findAllByIdInAndTopicOwnerId(Set.of(10L), 100L)).thenReturn(List.of(task));
        when(studySessionRepository.save(any(StudySession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudySessionResponse response = studySessionService.create(
                1L,
                new CreateStudySessionRequest(LocalDate.of(2026, 10, 11), 90, "REST API", Set.of(10L))
        );

        assertThat(response.topicId()).isEqualTo(1L);
        assertThat(response.completedTaskIds()).containsExactly(10L);
        assertThat(task.getStatus()).isEqualTo(LearningTaskStatus.COMPLETED);
        assertThat(task.isStatusManagedByStudySession()).isTrue();
    }

    @Test
    void createsSessionWithoutTakingOwnershipOfManuallyCompletedTask() {
        Topic topic = topic(1L, "Java");
        LearningTask task = task(10L, topic, LearningTaskStatus.COMPLETED);
        when(topicRepository.findByIdAndOwnerId(1L, 100L)).thenReturn(Optional.of(topic));
        when(learningTaskRepository.findAllByIdInAndTopicOwnerId(Set.of(10L), 100L)).thenReturn(List.of(task));
        when(studySessionRepository.save(any(StudySession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        studySessionService.create(
                1L,
                new CreateStudySessionRequest(LocalDate.of(2026, 10, 11), 90, "REST API", Set.of(10L))
        );

        assertThat(task.getStatus()).isEqualTo(LearningTaskStatus.COMPLETED);
        assertThat(task.isStatusManagedByStudySession()).isFalse();
    }

    @Test
    void allowsSessionWithoutCompletedTasks() {
        Topic topic = topic(1L, "Java");
        when(topicRepository.findByIdAndOwnerId(1L, 100L)).thenReturn(Optional.of(topic));
        when(studySessionRepository.save(any(StudySession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudySessionResponse response = studySessionService.create(
                1L,
                new CreateStudySessionRequest(LocalDate.of(2026, 10, 11), 30, null, Set.of())
        );

        assertThat(response.completedTaskIds()).isEmpty();
        verify(learningTaskRepository, never()).findAllByIdInAndTopicOwnerId(any(), any());
    }

    @Test
    void throwsWhenCreatingSessionForUnknownTopic() {
        when(topicRepository.findByIdAndOwnerId(99L, 100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studySessionService.create(
                99L, new CreateStudySessionRequest(LocalDate.now(), 30, null, Set.of())
        )).isInstanceOf(TopicNotFoundException.class);
    }

    @Test
    void throwsWhenCompletedTaskIsUnknown() {
        Topic topic = topic(1L, "Java");
        when(topicRepository.findByIdAndOwnerId(1L, 100L)).thenReturn(Optional.of(topic));
        when(learningTaskRepository.findAllByIdInAndTopicOwnerId(Set.of(99L), 100L)).thenReturn(List.of());

        assertThatThrownBy(() -> studySessionService.create(
                1L, new CreateStudySessionRequest(LocalDate.now(), 30, null, Set.of(99L))
        )).isInstanceOf(LearningTaskNotFoundException.class);
    }

    @Test
    void rejectsTaskFromAnotherTopic() {
        Topic java = topic(1L, "Java");
        Topic sql = topic(2L, "SQL");
        LearningTask sqlTask = task(10L, sql, LearningTaskStatus.PLANNED);
        when(topicRepository.findByIdAndOwnerId(1L, 100L)).thenReturn(Optional.of(java));
        when(learningTaskRepository.findAllByIdInAndTopicOwnerId(Set.of(10L), 100L)).thenReturn(List.of(sqlTask));

        assertThatThrownBy(() -> studySessionService.create(
                1L, new CreateStudySessionRequest(LocalDate.now(), 30, null, Set.of(10L))
        )).isInstanceOf(LearningTaskDoesNotBelongToTopicException.class);
    }

    @Test
    void returnsSessionsForTopic() {
        Topic topic = topic(1L, "Java");
        when(topicRepository.existsByIdAndOwnerId(1L, 100L)).thenReturn(true);
        when(studySessionRepository.findAllByTopicIdAndTopicOwnerIdOrderBySessionDateDesc(1L, 100L)).thenReturn(List.of(
                session(5L, topic, Set.of())
        ));

        List<StudySessionResponse> responses = studySessionService.getAllByTopicId(1L);

        assertThat(responses).extracting(StudySessionResponse::id).containsExactly(5L);
    }

    @Test
    void returnsFilteredSessionPage() {
        Topic topic = topic(1L, "Java");
        StudySession session = session(5L, topic, Set.of());
        PageRequest pageable = PageRequest.of(0, 10);
        when(studySessionRepository.findAll(
                org.mockito.ArgumentMatchers.<Specification<StudySession>>any(),
                eq(pageable)
        ))
                .thenReturn(new PageImpl<>(List.of(session), pageable, 1));

        var response = studySessionService.search(
                1L,
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31),
                pageable
        );

        assertThat(response.getContent()).extracting(StudySessionResponse::id).containsExactly(5L);
        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    @Test
    void rejectsReversedSessionDateRange() {
        PageRequest pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> studySessionService.search(
                null,
                LocalDate.of(2026, 10, 31),
                LocalDate.of(2026, 10, 1),
                pageable
        )).isInstanceOf(InvalidPageRequestException.class)
                .hasMessage("Date from must be earlier than or equal to date to");

        verify(studySessionRepository, never()).findAll(
                org.mockito.ArgumentMatchers.<Specification<StudySession>>any(),
                any(Pageable.class)
        );
    }

    @Test
    void rejectsNonPositiveTopicFilter() {
        assertThatThrownBy(() -> studySessionService.search(
                -1L, null, null, PageRequest.of(0, 10)
        )).isInstanceOf(InvalidPageRequestException.class)
                .hasMessage("Topic id must be greater than zero");
    }

    @Test
    void throwsWhenListingSessionsForUnknownTopic() {
        when(topicRepository.existsByIdAndOwnerId(99L, 100L)).thenReturn(false);

        assertThatThrownBy(() -> studySessionService.getAllByTopicId(99L))
                .isInstanceOf(TopicNotFoundException.class);

        verify(studySessionRepository, never())
                .findAllByTopicIdAndTopicOwnerIdOrderBySessionDateDesc(99L, 100L);
    }

    @Test
    void updatesSessionAndCompletesNewTask() {
        Topic topic = topic(1L, "Java");
        StudySession session = session(5L, topic, Set.of());
        LearningTask task = task(10L, topic, LearningTaskStatus.PLANNED);
        when(studySessionRepository.findByIdAndTopicOwnerId(5L, 100L)).thenReturn(Optional.of(session));
        when(learningTaskRepository.findAllByIdInAndTopicOwnerId(Set.of(10L), 100L)).thenReturn(List.of(task));

        StudySessionResponse response = studySessionService.update(
                5L,
                new UpdateStudySessionRequest(LocalDate.of(2026, 10, 10), 120, "Updated", Set.of(10L))
        );

        assertThat(response.durationMinutes()).isEqualTo(120);
        assertThat(task.getStatus()).isEqualTo(LearningTaskStatus.COMPLETED);
    }

    @Test
    void restoresPlannedStatusWhenUpdateRemovesTaskFromItsLastSession() {
        Topic topic = topic(1L, "Java");
        LearningTask task = task(10L, topic, LearningTaskStatus.PLANNED);
        task.completeFromStudySession();
        StudySession session = session(5L, topic, Set.of(task));
        when(studySessionRepository.findByIdAndTopicOwnerId(5L, 100L)).thenReturn(Optional.of(session));
        when(studySessionRepository.findTaskIdsUsedByOtherSessions(Set.of(10L), 5L, 100L))
                .thenReturn(Set.of());

        studySessionService.update(
                5L,
                new UpdateStudySessionRequest(LocalDate.of(2026, 10, 10), 120, "Updated", Set.of())
        );

        assertThat(task.getStatus()).isEqualTo(LearningTaskStatus.PLANNED);
        assertThat(task.isStatusManagedByStudySession()).isFalse();
    }

    @Test
    void keepsCompletedStatusWhenAnotherSessionStillContainsTask() {
        Topic topic = topic(1L, "Java");
        LearningTask task = task(10L, topic, LearningTaskStatus.IN_PROGRESS);
        task.completeFromStudySession();
        StudySession session = session(5L, topic, Set.of(task));
        when(studySessionRepository.findByIdAndTopicOwnerId(5L, 100L)).thenReturn(Optional.of(session));
        when(studySessionRepository.findTaskIdsUsedByOtherSessions(Set.of(10L), 5L, 100L))
                .thenReturn(Set.of(10L));

        studySessionService.update(
                5L,
                new UpdateStudySessionRequest(LocalDate.of(2026, 10, 10), 120, "Updated", Set.of())
        );

        assertThat(task.getStatus()).isEqualTo(LearningTaskStatus.COMPLETED);
        assertThat(task.isStatusManagedByStudySession()).isTrue();
    }

    @Test
    void keepsManuallyCompletedStatusWhenUpdateRemovesTask() {
        Topic topic = topic(1L, "Java");
        LearningTask task = task(10L, topic, LearningTaskStatus.COMPLETED);
        StudySession session = session(5L, topic, Set.of(task));
        when(studySessionRepository.findByIdAndTopicOwnerId(5L, 100L)).thenReturn(Optional.of(session));

        studySessionService.update(
                5L,
                new UpdateStudySessionRequest(LocalDate.of(2026, 10, 10), 120, "Updated", Set.of())
        );

        assertThat(task.getStatus()).isEqualTo(LearningTaskStatus.COMPLETED);
        verify(studySessionRepository, never()).findTaskIdsUsedByOtherSessions(any(), any(), any());
    }

    @Test
    void throwsWhenGettingUnknownSession() {
        when(studySessionRepository.findByIdAndTopicOwnerId(99L, 100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studySessionService.getById(99L))
                .isInstanceOf(StudySessionNotFoundException.class);
    }

    @Test
    void hidesSessionOwnedByAnotherUser() {
        User anotherUser = new User("another@example.com", "password-hash");
        ReflectionTestUtils.setField(anotherUser, "id", 200L);
        when(currentUserProvider.getCurrentUser()).thenReturn(anotherUser);
        when(studySessionRepository.findByIdAndTopicOwnerId(5L, 200L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studySessionService.getById(5L))
                .isInstanceOf(StudySessionNotFoundException.class);

        verify(studySessionRepository, never()).findById(5L);
    }

    @Test
    void deletesSession() {
        Topic topic = topic(1L, "Java");
        StudySession session = session(5L, topic, Set.of());
        when(studySessionRepository.findByIdAndTopicOwnerId(5L, 100L)).thenReturn(Optional.of(session));

        studySessionService.delete(5L);

        verify(studySessionRepository).delete(session);
    }

    @Test
    void restoresInProgressStatusWhenDeletingTasksLastSession() {
        Topic topic = topic(1L, "Java");
        LearningTask task = task(10L, topic, LearningTaskStatus.IN_PROGRESS);
        task.completeFromStudySession();
        StudySession session = session(5L, topic, Set.of(task));
        when(studySessionRepository.findByIdAndTopicOwnerId(5L, 100L)).thenReturn(Optional.of(session));
        when(studySessionRepository.findTaskIdsUsedByOtherSessions(Set.of(10L), 5L, 100L))
                .thenReturn(Set.of());

        studySessionService.delete(5L);

        assertThat(task.getStatus()).isEqualTo(LearningTaskStatus.IN_PROGRESS);
        verify(studySessionRepository).delete(session);
    }

    @Test
    void loadsCompletedTasksInOneBatch() {
        Topic topic = topic(1L, "Java");
        LearningTask firstTask = task(10L, topic, LearningTaskStatus.PLANNED);
        LearningTask secondTask = task(11L, topic, LearningTaskStatus.IN_PROGRESS);
        Set<Long> taskIds = new LinkedHashSet<>(List.of(10L, 11L));
        when(topicRepository.findByIdAndOwnerId(1L, 100L)).thenReturn(Optional.of(topic));
        when(learningTaskRepository.findAllByIdInAndTopicOwnerId(taskIds, 100L))
                .thenReturn(List.of(secondTask, firstTask));
        when(studySessionRepository.save(any(StudySession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StudySessionResponse response = studySessionService.create(
                1L,
                new CreateStudySessionRequest(LocalDate.of(2026, 10, 12), 60, null, taskIds)
        );

        assertThat(response.completedTaskIds()).containsExactlyInAnyOrder(10L, 11L);
        assertThat(firstTask.getStatus()).isEqualTo(LearningTaskStatus.COMPLETED);
        assertThat(secondTask.getStatus()).isEqualTo(LearningTaskStatus.COMPLETED);
        verify(learningTaskRepository).findAllByIdInAndTopicOwnerId(taskIds, 100L);
        verify(learningTaskRepository, never()).findById(any());
    }

    @Test
    void reportsMissingTaskWhenBatchResultIsPartial() {
        Topic topic = topic(1L, "Java");
        LearningTask existingTask = task(10L, topic, LearningTaskStatus.PLANNED);
        Set<Long> taskIds = new LinkedHashSet<>(List.of(10L, 99L));
        when(topicRepository.findByIdAndOwnerId(1L, 100L)).thenReturn(Optional.of(topic));
        when(learningTaskRepository.findAllByIdInAndTopicOwnerId(taskIds, 100L))
                .thenReturn(List.of(existingTask));

        assertThatThrownBy(() -> studySessionService.create(
                1L,
                new CreateStudySessionRequest(LocalDate.of(2026, 10, 12), 60, null, taskIds)
        )).isInstanceOf(LearningTaskNotFoundException.class)
                .hasMessage("Learning task with id 99 was not found");

        assertThat(existingTask.getStatus()).isEqualTo(LearningTaskStatus.PLANNED);
        verify(studySessionRepository, never()).save(any(StudySession.class));
    }

    private Topic topic(Long id, String title) {
        Topic topic = new Topic(currentUser, title, null);
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
