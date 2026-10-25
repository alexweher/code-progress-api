package com.example.codeprogress.learningtask.service;

import com.example.codeprogress.learningtask.LearningTask;
import com.example.codeprogress.common.pagination.InvalidPageRequestException;
import com.example.codeprogress.learningtask.LearningTaskDifficulty;
import com.example.codeprogress.learningtask.LearningTaskPriority;
import com.example.codeprogress.learningtask.LearningTaskStatus;
import com.example.codeprogress.learningtask.dto.CreateLearningTaskRequest;
import com.example.codeprogress.learningtask.dto.LearningTaskResponse;
import com.example.codeprogress.learningtask.dto.UpdateLearningTaskRequest;
import com.example.codeprogress.learningtask.dto.UpdateLearningTaskPlanningRequest;
import com.example.codeprogress.learningtask.dto.UpdateLearningTaskStatusRequest;
import com.example.codeprogress.learningtask.exception.LearningTaskDeletionConflictException;
import com.example.codeprogress.learningtask.exception.LearningTaskNotFoundException;
import com.example.codeprogress.learningtask.repository.LearningTaskRepository;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningTaskServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-10-19T22:30:00Z"),
            ZoneId.of("Europe/Moscow")
    );

    @Mock
    private LearningTaskRepository learningTaskRepository;

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private StudySessionRepository studySessionRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    private LearningTaskService learningTaskService;
    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = new User("owner@example.com", "password-hash");
        ReflectionTestUtils.setField(currentUser, "id", 100L);
        lenient().when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
        learningTaskService = new LearningTaskService(
                learningTaskRepository,
                topicRepository,
                studySessionRepository,
                currentUserProvider,
                CLOCK
        );
    }

    @Test
    void createsTaskWithPlannedStatus() {
        Topic topic = topic(1L);
        when(topicRepository.findByIdAndOwnerId(1L, 100L)).thenReturn(Optional.of(topic));
        when(learningTaskRepository.save(any(LearningTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LearningTaskResponse response = learningTaskService.create(
                1L,
                new CreateLearningTaskRequest("Implement REST API", "Build task endpoints", LearningTaskDifficulty.MEDIUM)
        );

        assertThat(response.topicId()).isEqualTo(1L);
        assertThat(response.difficulty()).isEqualTo(LearningTaskDifficulty.MEDIUM);
        assertThat(response.status()).isEqualTo(LearningTaskStatus.PLANNED);
        verify(learningTaskRepository).save(any(LearningTask.class));
    }

    @Test
    void createsTaskWithDeadlineAndPriority() {
        Topic topic = topic(1L);
        LocalDate deadline = LocalDate.of(2026, 10, 25);
        when(topicRepository.findByIdAndOwnerId(1L, 100L)).thenReturn(Optional.of(topic));
        when(learningTaskRepository.save(any(LearningTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LearningTaskResponse response = learningTaskService.create(
                1L,
                new CreateLearningTaskRequest(
                        "Implement planning",
                        null,
                        LearningTaskDifficulty.MEDIUM,
                        deadline,
                        LearningTaskPriority.HIGH
                )
        );

        assertThat(response.deadline()).isEqualTo(deadline);
        assertThat(response.priority()).isEqualTo(LearningTaskPriority.HIGH);
        assertThat(response.status()).isEqualTo(LearningTaskStatus.PLANNED);
    }

    @Test
    void usesMediumPriorityWhenCreateRequestOmitsPriority() {
        Topic topic = topic(1L);
        when(topicRepository.findByIdAndOwnerId(1L, 100L)).thenReturn(Optional.of(topic));
        when(learningTaskRepository.save(any(LearningTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LearningTaskResponse response = learningTaskService.create(
                1L,
                new CreateLearningTaskRequest(
                        "Legacy client task",
                        null,
                        LearningTaskDifficulty.EASY,
                        null,
                        null
                )
        );

        assertThat(response.deadline()).isNull();
        assertThat(response.priority()).isEqualTo(LearningTaskPriority.MEDIUM);
    }

    @Test
    void throwsWhenCreatingTaskForUnknownTopic() {
        when(topicRepository.findByIdAndOwnerId(99L, 100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> learningTaskService.create(
                99L,
                new CreateLearningTaskRequest("Task", null, LearningTaskDifficulty.EASY)
        )).isInstanceOf(TopicNotFoundException.class);
        verify(learningTaskRepository, never()).save(any(LearningTask.class));
    }

    @Test
    void returnsTasksForTopic() {
        Topic topic = topic(1L);
        when(topicRepository.findByIdAndOwnerId(1L, 100L)).thenReturn(Optional.of(topic));
        when(learningTaskRepository.findAllByTopicIdAndTopicOwnerId(1L, 100L)).thenReturn(List.of(
                task(10L, topic, "First", LearningTaskDifficulty.EASY, LearningTaskStatus.PLANNED),
                task(11L, topic, "Second", LearningTaskDifficulty.HARD, LearningTaskStatus.IN_PROGRESS)
        ));

        List<LearningTaskResponse> responses = learningTaskService.getAllByTopicId(1L);

        assertThat(responses).extracting(LearningTaskResponse::id).containsExactly(10L, 11L);
        assertThat(responses).extracting(LearningTaskResponse::title).containsExactly("First", "Second");
    }

    @Test
    void returnsFilteredTaskPage() {
        Topic topic = topic(1L);
        LearningTask task = task(10L, topic, "Task", LearningTaskDifficulty.HARD, LearningTaskStatus.COMPLETED);
        PageRequest pageable = PageRequest.of(0, 10);
        when(learningTaskRepository.findAll(
                org.mockito.ArgumentMatchers.<Specification<LearningTask>>any(),
                eq(pageable)
        ))
                .thenReturn(new PageImpl<>(List.of(task), pageable, 1));

        var response = learningTaskService.search(
                1L,
                LearningTaskStatus.COMPLETED,
                LearningTaskDifficulty.HARD,
                pageable
        );

        assertThat(response.getContent()).extracting(LearningTaskResponse::id).containsExactly(10L);
        assertThat(response.getTotalElements()).isEqualTo(1);
        verify(learningTaskRepository).findAll(
                org.mockito.ArgumentMatchers.<Specification<LearningTask>>any(),
                eq(pageable)
        );
    }

    @Test
    void filtersTasksByDeadlinePriorityAndStatus() {
        Topic topic = topic(1L);
        LearningTask task = task(10L, topic, "Task", LearningTaskDifficulty.HARD, LearningTaskStatus.IN_PROGRESS);
        task.updatePlanning(LocalDate.of(2026, 10, 22), LearningTaskPriority.HIGH);
        PageRequest pageable = PageRequest.of(0, 10);
        when(learningTaskRepository.findAll(
                org.mockito.ArgumentMatchers.<Specification<LearningTask>>any(),
                eq(pageable)
        )).thenReturn(new PageImpl<>(List.of(task), pageable, 1));

        var response = learningTaskService.search(
                1L,
                LearningTaskStatus.IN_PROGRESS,
                LearningTaskDifficulty.HARD,
                LearningTaskPriority.HIGH,
                LocalDate.of(2026, 10, 20),
                LocalDate.of(2026, 10, 25),
                pageable
        );

        assertThat(response.getContent()).singleElement().satisfies(result -> {
            assertThat(result.deadline()).isEqualTo(LocalDate.of(2026, 10, 22));
            assertThat(result.priority()).isEqualTo(LearningTaskPriority.HIGH);
        });
    }

    @Test
    void rejectsReversedDeadlineFilter() {
        assertThatThrownBy(() -> learningTaskService.search(
                null,
                null,
                null,
                null,
                LocalDate.of(2026, 10, 21),
                LocalDate.of(2026, 10, 20),
                PageRequest.of(0, 10)
        )).isInstanceOf(InvalidPageRequestException.class)
                .hasMessage("Deadline from must be earlier than or equal to deadline to");

        verify(learningTaskRepository, never()).findAll(
                org.mockito.ArgumentMatchers.<Specification<LearningTask>>any(),
                any(PageRequest.class)
        );
    }

    @Test
    void rejectsNonPositiveTopicFilter() {
        assertThatThrownBy(() -> learningTaskService.search(
                0L, null, null, PageRequest.of(0, 10)
        )).isInstanceOf(InvalidPageRequestException.class)
                .hasMessage("Topic id must be greater than zero");
    }

    @Test
    void throwsWhenGettingTasksForUnknownTopic() {
        when(topicRepository.findByIdAndOwnerId(99L, 100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> learningTaskService.getAllByTopicId(99L))
                .isInstanceOf(TopicNotFoundException.class);
    }

    @Test
    void returnsTaskById() {
        Topic topic = topic(1L);
        when(learningTaskRepository.findByIdAndTopicOwnerId(10L, 100L)).thenReturn(Optional.of(
                task(10L, topic, "Task", LearningTaskDifficulty.EASY, LearningTaskStatus.PLANNED)
        ));

        LearningTaskResponse response = learningTaskService.getById(10L);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.topicId()).isEqualTo(1L);
    }

    @Test
    void throwsWhenGettingUnknownTask() {
        when(learningTaskRepository.findByIdAndTopicOwnerId(99L, 100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> learningTaskService.getById(99L))
                .isInstanceOf(LearningTaskNotFoundException.class)
                .hasMessage("Learning task with id 99 was not found");
    }

    @Test
    void updatesTaskDetailsWithoutChangingStatus() {
        Topic topic = topic(1L);
        LearningTask task = task(10L, topic, "Old", LearningTaskDifficulty.EASY, LearningTaskStatus.IN_PROGRESS);
        when(learningTaskRepository.findByIdAndTopicOwnerId(10L, 100L)).thenReturn(Optional.of(task));

        LearningTaskResponse response = learningTaskService.update(
                10L,
                new UpdateLearningTaskRequest("New", "New description", LearningTaskDifficulty.HARD)
        );

        assertThat(response.title()).isEqualTo("New");
        assertThat(response.difficulty()).isEqualTo(LearningTaskDifficulty.HARD);
        assertThat(response.status()).isEqualTo(LearningTaskStatus.IN_PROGRESS);
    }

    @Test
    void hidesTaskOwnedByAnotherUser() {
        User anotherUser = new User("another@example.com", "password-hash");
        ReflectionTestUtils.setField(anotherUser, "id", 200L);
        when(currentUserProvider.getCurrentUser()).thenReturn(anotherUser);
        when(learningTaskRepository.findByIdAndTopicOwnerId(10L, 200L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> learningTaskService.getById(10L))
                .isInstanceOf(LearningTaskNotFoundException.class);

        verify(learningTaskRepository, never()).findById(10L);
    }

    @Test
    void updatesOnlyTaskStatus() {
        Topic topic = topic(1L);
        LearningTask task = task(10L, topic, "Task", LearningTaskDifficulty.MEDIUM, LearningTaskStatus.PLANNED);
        when(learningTaskRepository.findByIdAndTopicOwnerId(10L, 100L)).thenReturn(Optional.of(task));

        LearningTaskResponse response = learningTaskService.updateStatus(
                10L,
                new UpdateLearningTaskStatusRequest(LearningTaskStatus.COMPLETED)
        );

        assertThat(response.status()).isEqualTo(LearningTaskStatus.COMPLETED);
        assertThat(response.difficulty()).isEqualTo(LearningTaskDifficulty.MEDIUM);
    }

    @Test
    void updatesPlanningWithoutChangingTaskStatus() {
        Topic topic = topic(1L);
        LearningTask task = task(10L, topic, "Task", LearningTaskDifficulty.MEDIUM, LearningTaskStatus.IN_PROGRESS);
        when(learningTaskRepository.findByIdAndTopicOwnerId(10L, 100L)).thenReturn(Optional.of(task));

        LearningTaskResponse response = learningTaskService.updatePlanning(
                10L,
                new UpdateLearningTaskPlanningRequest(
                        LocalDate.of(2026, 10, 25),
                        LearningTaskPriority.HIGH
                )
        );

        assertThat(response.deadline()).isEqualTo(LocalDate.of(2026, 10, 25));
        assertThat(response.priority()).isEqualTo(LearningTaskPriority.HIGH);
        assertThat(response.status()).isEqualTo(LearningTaskStatus.IN_PROGRESS);
    }

    @Test
    void clearsDeadlineThroughPlanningUpdate() {
        Topic topic = topic(1L);
        LearningTask task = task(10L, topic, "Task", LearningTaskDifficulty.MEDIUM, LearningTaskStatus.PLANNED);
        task.updatePlanning(LocalDate.of(2026, 10, 25), LearningTaskPriority.HIGH);
        when(learningTaskRepository.findByIdAndTopicOwnerId(10L, 100L)).thenReturn(Optional.of(task));

        LearningTaskResponse response = learningTaskService.updatePlanning(
                10L,
                new UpdateLearningTaskPlanningRequest(null, LearningTaskPriority.LOW)
        );

        assertThat(response.deadline()).isNull();
        assertThat(response.priority()).isEqualTo(LearningTaskPriority.LOW);
    }

    @Test
    void returnsDueTodayUpcomingAndOverdueWithoutChangingStatuses() {
        Topic topic = topic(1L);
        LearningTask task = task(10L, topic, "Task", LearningTaskDifficulty.MEDIUM, LearningTaskStatus.IN_PROGRESS);
        task.updatePlanning(LocalDate.of(2026, 10, 19), LearningTaskPriority.HIGH);
        PageRequest pageable = PageRequest.of(0, 10);
        PageImpl<LearningTask> result = new PageImpl<>(List.of(task), pageable, 1);
        when(learningTaskRepository.findAllByTopicOwnerIdAndStatusNotAndDeadline(
                100L, LearningTaskStatus.COMPLETED, LocalDate.of(2026, 10, 20), pageable
        )).thenReturn(result);
        when(learningTaskRepository.findAllByTopicOwnerIdAndStatusNotAndDeadlineBetween(
                100L,
                LearningTaskStatus.COMPLETED,
                LocalDate.of(2026, 10, 20),
                LocalDate.of(2026, 10, 26),
                pageable
        )).thenReturn(result);
        when(learningTaskRepository.findAllByTopicOwnerIdAndStatusNotAndDeadlineBefore(
                100L, LearningTaskStatus.COMPLETED, LocalDate.of(2026, 10, 20), pageable
        )).thenReturn(result);

        assertThat(learningTaskService.getDueToday(pageable).getContent()).hasSize(1);
        assertThat(learningTaskService.getUpcoming(pageable).getContent()).hasSize(1);
        LearningTaskResponse overdue = learningTaskService.getOverdue(pageable).getContent().getFirst();

        assertThat(overdue.status()).isEqualTo(LearningTaskStatus.IN_PROGRESS);
        assertThat(task.getStatus()).isEqualTo(LearningTaskStatus.IN_PROGRESS);
        verify(learningTaskRepository).findAllByTopicOwnerIdAndStatusNotAndDeadlineBetween(
                100L,
                LearningTaskStatus.COMPLETED,
                LocalDate.of(2026, 10, 20),
                LocalDate.of(2026, 10, 26),
                pageable
        );
        verify(learningTaskRepository).findAllByTopicOwnerIdAndStatusNotAndDeadlineBefore(
                100L, LearningTaskStatus.COMPLETED, LocalDate.of(2026, 10, 20), pageable
        );
    }

    @Test
    void scopesPlanningListsToCurrentUser() {
        User anotherUser = new User("another@example.com", "password-hash");
        ReflectionTestUtils.setField(anotherUser, "id", 200L);
        when(currentUserProvider.getCurrentUser()).thenReturn(anotherUser);
        PageRequest pageable = PageRequest.of(0, 10);
        when(learningTaskRepository.findAllByTopicOwnerIdAndStatusNotAndDeadlineBefore(
                200L, LearningTaskStatus.COMPLETED, LocalDate.of(2026, 10, 20), pageable
        )).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        assertThat(learningTaskService.getOverdue(pageable)).isEmpty();

        verify(learningTaskRepository).findAllByTopicOwnerIdAndStatusNotAndDeadlineBefore(
                200L, LearningTaskStatus.COMPLETED, LocalDate.of(2026, 10, 20), pageable
        );
    }

    @Test
    void manualStatusUpdateOverridesStudySessionManagedStatus() {
        Topic topic = topic(1L);
        LearningTask task = task(10L, topic, "Task", LearningTaskDifficulty.MEDIUM, LearningTaskStatus.IN_PROGRESS);
        task.completeFromStudySession();
        when(learningTaskRepository.findByIdAndTopicOwnerId(10L, 100L)).thenReturn(Optional.of(task));

        LearningTaskResponse response = learningTaskService.updateStatus(
                10L,
                new UpdateLearningTaskStatusRequest(LearningTaskStatus.COMPLETED)
        );

        assertThat(response.status()).isEqualTo(LearningTaskStatus.COMPLETED);
        assertThat(task.isStatusManagedByStudySession()).isFalse();
    }

    @Test
    void deletesExistingTask() {
        Topic topic = topic(1L);
        LearningTask task = task(10L, topic, "Task", LearningTaskDifficulty.EASY, LearningTaskStatus.PLANNED);
        when(learningTaskRepository.findByIdAndTopicOwnerId(10L, 100L)).thenReturn(Optional.of(task));

        learningTaskService.delete(10L);

        verify(learningTaskRepository).delete(task);
    }

    @Test
    void doesNotDeleteTaskUsedInStudySessionHistory() {
        Topic topic = topic(1L);
        LearningTask task = task(10L, topic, "Task", LearningTaskDifficulty.EASY, LearningTaskStatus.COMPLETED);
        when(learningTaskRepository.findByIdAndTopicOwnerId(10L, 100L)).thenReturn(Optional.of(task));
        when(studySessionRepository.existsByCompletedTasks_IdAndTopicOwnerId(10L, 100L)).thenReturn(true);

        assertThatThrownBy(() -> learningTaskService.delete(10L))
                .isInstanceOf(LearningTaskDeletionConflictException.class)
                .hasMessage("Learning task with id 10 cannot be deleted because it is part of study session history");

        verify(learningTaskRepository, never()).delete(any(LearningTask.class));
    }

    private Topic topic(Long id) {
        Topic topic = new Topic(currentUser, "Java", null);
        ReflectionTestUtils.setField(topic, "id", id);
        return topic;
    }

    private LearningTask task(
            Long id,
            Topic topic,
            String title,
            LearningTaskDifficulty difficulty,
            LearningTaskStatus status
    ) {
        LearningTask task = new LearningTask(topic, title, "Description", difficulty);
        if (status != LearningTaskStatus.PLANNED) {
            task.updateStatus(status);
        }
        ReflectionTestUtils.setField(task, "id", id);
        ReflectionTestUtils.setField(task, "createdAt", LocalDateTime.now().minusDays(2));
        ReflectionTestUtils.setField(task, "updatedAt", LocalDateTime.now().minusDays(1));
        return task;
    }
}
