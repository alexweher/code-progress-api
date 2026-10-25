package com.example.codeprogress.topic.service;

import com.example.codeprogress.learningtask.repository.LearningTaskRepository;
import com.example.codeprogress.studysession.repository.StudySessionRepository;
import com.example.codeprogress.topic.Topic;
import com.example.codeprogress.topic.TopicStatus;
import com.example.codeprogress.topic.dto.CreateTopicRequest;
import com.example.codeprogress.topic.dto.TopicResponse;
import com.example.codeprogress.topic.dto.UpdateTopicRequest;
import com.example.codeprogress.topic.dto.UpdateTopicStatusRequest;
import com.example.codeprogress.topic.exception.TopicDeletionConflictException;
import com.example.codeprogress.topic.exception.TopicNotFoundException;
import com.example.codeprogress.topic.repository.TopicRepository;
import com.example.codeprogress.user.User;
import com.example.codeprogress.user.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
class TopicServiceTest {

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private LearningTaskRepository learningTaskRepository;

    @Mock
    private StudySessionRepository studySessionRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    private TopicService topicService;
    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = new User("owner@example.com", "password-hash");
        ReflectionTestUtils.setField(currentUser, "id", 100L);
        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
        topicService = new TopicService(
                topicRepository,
                learningTaskRepository,
                studySessionRepository,
                currentUserProvider
        );
    }

    @Test
    void createsTopicWithPlannedStatus() {
        when(topicRepository.save(any(Topic.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TopicResponse response = topicService.create(
                new CreateTopicRequest("Spring Boot", "Learn the fundamentals")
        );

        assertThat(response.title()).isEqualTo("Spring Boot");
        assertThat(response.description()).isEqualTo("Learn the fundamentals");
        assertThat(response.status()).isEqualTo(TopicStatus.PLANNED);
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.updatedAt()).isNotNull();
        ArgumentCaptor<Topic> topicCaptor = ArgumentCaptor.forClass(Topic.class);
        verify(topicRepository).save(topicCaptor.capture());
        assertThat(topicCaptor.getValue().getOwner()).isSameAs(currentUser);
    }

    @Test
    void hidesTopicOwnedByAnotherUser() {
        User anotherUser = new User("another@example.com", "password-hash");
        ReflectionTestUtils.setField(anotherUser, "id", 200L);
        when(currentUserProvider.getCurrentUser()).thenReturn(anotherUser);
        when(topicRepository.findByIdAndOwnerId(1L, 200L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> topicService.getById(1L))
                .isInstanceOf(TopicNotFoundException.class);

        verify(topicRepository, never()).findById(1L);
    }

    @Test
    void returnsAllTopicsAsResponses() {
        Topic first = topic(1L, "Java", null, TopicStatus.PLANNED);
        Topic second = topic(2L, "SQL", "PostgreSQL", TopicStatus.IN_PROGRESS);
        when(topicRepository.findAllByOwnerId(100L)).thenReturn(List.of(first, second));

        List<TopicResponse> responses = topicService.getAll();

        assertThat(responses).extracting(TopicResponse::id).containsExactly(1L, 2L);
        assertThat(responses).extracting(TopicResponse::title).containsExactly("Java", "SQL");
    }

    @Test
    void returnsPagedTopics() {
        PageRequest pageable = PageRequest.of(0, 10);
        Topic topic = topic(1L, "Java", null, TopicStatus.PLANNED);
        when(topicRepository.findAllByOwnerId(100L, pageable))
                .thenReturn(new PageImpl<>(List.of(topic), pageable, 1));

        var response = topicService.search(pageable);

        assertThat(response.getContent()).extracting(TopicResponse::id).containsExactly(1L);
        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    @Test
    void returnsTopicById() {
        when(topicRepository.findByIdAndOwnerId(1L, 100L))
                .thenReturn(Optional.of(topic(1L, "Java", "Core Java", TopicStatus.PLANNED)));

        TopicResponse response = topicService.getById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("Java");
    }

    @Test
    void throwsWhenGettingUnknownTopic() {
        when(topicRepository.findByIdAndOwnerId(99L, 100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> topicService.getById(99L))
                .isInstanceOf(TopicNotFoundException.class)
                .hasMessage("Topic with id 99 was not found");
    }

    @Test
    void updatesTopicDetailsWithoutChangingStatus() {
        Topic topic = topic(1L, "Old title", "Old description", TopicStatus.IN_PROGRESS);
        LocalDateTime previousUpdatedAt = topic.getUpdatedAt();
        when(topicRepository.findByIdAndOwnerId(1L, 100L)).thenReturn(Optional.of(topic));

        TopicResponse response = topicService.update(
                1L,
                new UpdateTopicRequest("New title", "New description")
        );

        assertThat(response.title()).isEqualTo("New title");
        assertThat(response.description()).isEqualTo("New description");
        assertThat(response.status()).isEqualTo(TopicStatus.IN_PROGRESS);
        assertThat(response.updatedAt()).isAfter(previousUpdatedAt);
    }

    @Test
    void throwsWhenUpdatingUnknownTopic() {
        when(topicRepository.findByIdAndOwnerId(99L, 100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> topicService.update(99L, new UpdateTopicRequest("Java", null)))
                .isInstanceOf(TopicNotFoundException.class);
    }

    @Test
    void updatesOnlyTopicStatus() {
        Topic topic = topic(1L, "Java", "Core Java", TopicStatus.PLANNED);
        LocalDateTime previousUpdatedAt = topic.getUpdatedAt();
        when(topicRepository.findByIdAndOwnerId(1L, 100L)).thenReturn(Optional.of(topic));

        TopicResponse response = topicService.updateStatus(
                1L,
                new UpdateTopicStatusRequest(TopicStatus.COMPLETED)
        );

        assertThat(response.title()).isEqualTo("Java");
        assertThat(response.description()).isEqualTo("Core Java");
        assertThat(response.status()).isEqualTo(TopicStatus.COMPLETED);
        assertThat(response.updatedAt()).isAfter(previousUpdatedAt);
    }

    @Test
    void throwsWhenUpdatingStatusOfUnknownTopic() {
        when(topicRepository.findByIdAndOwnerId(99L, 100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> topicService.updateStatus(
                99L,
                new UpdateTopicStatusRequest(TopicStatus.COMPLETED)
        )).isInstanceOf(TopicNotFoundException.class);
    }

    @Test
    void deletesExistingTopic() {
        Topic topic = topic(1L, "Java", null, TopicStatus.PLANNED);
        when(topicRepository.findByIdAndOwnerId(1L, 100L)).thenReturn(Optional.of(topic));

        topicService.delete(1L);

        verify(topicRepository).delete(topic);
    }

    @Test
    void throwsWhenDeletingUnknownTopic() {
        when(topicRepository.findByIdAndOwnerId(99L, 100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> topicService.delete(99L))
                .isInstanceOf(TopicNotFoundException.class);
        verify(topicRepository, never()).delete(any(Topic.class));
    }

    @Test
    void doesNotDeleteTopicWithLearningTasks() {
        Topic topic = topic(1L, "Java", null, TopicStatus.PLANNED);
        when(topicRepository.findByIdAndOwnerId(1L, 100L)).thenReturn(Optional.of(topic));
        when(learningTaskRepository.existsByTopicIdAndTopicOwnerId(1L, 100L)).thenReturn(true);

        assertThatThrownBy(() -> topicService.delete(1L))
                .isInstanceOf(TopicDeletionConflictException.class)
                .hasMessage("Topic with id 1 cannot be deleted because it has learning tasks or study sessions");

        verify(topicRepository, never()).delete(any(Topic.class));
        verify(studySessionRepository, never()).existsByTopicIdAndTopicOwnerId(1L, 100L);
    }

    @Test
    void doesNotDeleteTopicWithStudySessions() {
        Topic topic = topic(1L, "Java", null, TopicStatus.COMPLETED);
        when(topicRepository.findByIdAndOwnerId(1L, 100L)).thenReturn(Optional.of(topic));
        when(studySessionRepository.existsByTopicIdAndTopicOwnerId(1L, 100L)).thenReturn(true);

        assertThatThrownBy(() -> topicService.delete(1L))
                .isInstanceOf(TopicDeletionConflictException.class);

        verify(topicRepository, never()).delete(any(Topic.class));
    }

    private Topic topic(
            Long id,
            String title,
            String description,
            TopicStatus status
    ) {
        Topic topic = new Topic(currentUser, title, description);
        if (status != TopicStatus.PLANNED) {
            topic.updateStatus(status);
        }
        ReflectionTestUtils.setField(topic, "id", id);
        ReflectionTestUtils.setField(topic, "createdAt", LocalDateTime.now().minusDays(2));
        ReflectionTestUtils.setField(topic, "updatedAt", LocalDateTime.now().minusDays(1));
        return topic;
    }
}
