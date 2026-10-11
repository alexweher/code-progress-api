package com.example.codeprogress.learningtask.service;

import com.example.codeprogress.learningtask.LearningTask;
import com.example.codeprogress.learningtask.LearningTaskDifficulty;
import com.example.codeprogress.learningtask.LearningTaskStatus;
import com.example.codeprogress.learningtask.dto.CreateLearningTaskRequest;
import com.example.codeprogress.learningtask.dto.LearningTaskResponse;
import com.example.codeprogress.learningtask.dto.UpdateLearningTaskRequest;
import com.example.codeprogress.learningtask.dto.UpdateLearningTaskStatusRequest;
import com.example.codeprogress.learningtask.exception.LearningTaskNotFoundException;
import com.example.codeprogress.learningtask.repository.LearningTaskRepository;
import com.example.codeprogress.topic.Topic;
import com.example.codeprogress.topic.exception.TopicNotFoundException;
import com.example.codeprogress.topic.repository.TopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningTaskServiceTest {

    @Mock
    private LearningTaskRepository learningTaskRepository;

    @Mock
    private TopicRepository topicRepository;

    private LearningTaskService learningTaskService;

    @BeforeEach
    void setUp() {
        learningTaskService = new LearningTaskService(learningTaskRepository, topicRepository);
    }

    @Test
    void createsTaskWithPlannedStatus() {
        Topic topic = topic(1L);
        when(topicRepository.findById(1L)).thenReturn(Optional.of(topic));
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
    void throwsWhenCreatingTaskForUnknownTopic() {
        when(topicRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> learningTaskService.create(
                99L,
                new CreateLearningTaskRequest("Task", null, LearningTaskDifficulty.EASY)
        )).isInstanceOf(TopicNotFoundException.class);
        verify(learningTaskRepository, never()).save(any(LearningTask.class));
    }

    @Test
    void returnsTasksForTopic() {
        Topic topic = topic(1L);
        when(topicRepository.findById(1L)).thenReturn(Optional.of(topic));
        when(learningTaskRepository.findAllByTopicId(1L)).thenReturn(List.of(
                task(10L, topic, "First", LearningTaskDifficulty.EASY, LearningTaskStatus.PLANNED),
                task(11L, topic, "Second", LearningTaskDifficulty.HARD, LearningTaskStatus.IN_PROGRESS)
        ));

        List<LearningTaskResponse> responses = learningTaskService.getAllByTopicId(1L);

        assertThat(responses).extracting(LearningTaskResponse::id).containsExactly(10L, 11L);
        assertThat(responses).extracting(LearningTaskResponse::title).containsExactly("First", "Second");
    }

    @Test
    void throwsWhenGettingTasksForUnknownTopic() {
        when(topicRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> learningTaskService.getAllByTopicId(99L))
                .isInstanceOf(TopicNotFoundException.class);
    }

    @Test
    void returnsTaskById() {
        Topic topic = topic(1L);
        when(learningTaskRepository.findById(10L)).thenReturn(Optional.of(
                task(10L, topic, "Task", LearningTaskDifficulty.EASY, LearningTaskStatus.PLANNED)
        ));

        LearningTaskResponse response = learningTaskService.getById(10L);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.topicId()).isEqualTo(1L);
    }

    @Test
    void throwsWhenGettingUnknownTask() {
        when(learningTaskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> learningTaskService.getById(99L))
                .isInstanceOf(LearningTaskNotFoundException.class)
                .hasMessage("Learning task with id 99 was not found");
    }

    @Test
    void updatesTaskDetailsWithoutChangingStatus() {
        Topic topic = topic(1L);
        LearningTask task = task(10L, topic, "Old", LearningTaskDifficulty.EASY, LearningTaskStatus.IN_PROGRESS);
        when(learningTaskRepository.findById(10L)).thenReturn(Optional.of(task));

        LearningTaskResponse response = learningTaskService.update(
                10L,
                new UpdateLearningTaskRequest("New", "New description", LearningTaskDifficulty.HARD)
        );

        assertThat(response.title()).isEqualTo("New");
        assertThat(response.difficulty()).isEqualTo(LearningTaskDifficulty.HARD);
        assertThat(response.status()).isEqualTo(LearningTaskStatus.IN_PROGRESS);
    }

    @Test
    void updatesOnlyTaskStatus() {
        Topic topic = topic(1L);
        LearningTask task = task(10L, topic, "Task", LearningTaskDifficulty.MEDIUM, LearningTaskStatus.PLANNED);
        when(learningTaskRepository.findById(10L)).thenReturn(Optional.of(task));

        LearningTaskResponse response = learningTaskService.updateStatus(
                10L,
                new UpdateLearningTaskStatusRequest(LearningTaskStatus.COMPLETED)
        );

        assertThat(response.status()).isEqualTo(LearningTaskStatus.COMPLETED);
        assertThat(response.difficulty()).isEqualTo(LearningTaskDifficulty.MEDIUM);
    }

    @Test
    void deletesExistingTask() {
        Topic topic = topic(1L);
        LearningTask task = task(10L, topic, "Task", LearningTaskDifficulty.EASY, LearningTaskStatus.PLANNED);
        when(learningTaskRepository.findById(10L)).thenReturn(Optional.of(task));

        learningTaskService.delete(10L);

        verify(learningTaskRepository).delete(task);
    }

    private Topic topic(Long id) {
        Topic topic = new Topic("Java", null);
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
