package com.example.codeprogress.topic.service;

import com.example.codeprogress.topic.Topic;
import com.example.codeprogress.topic.TopicStatus;
import com.example.codeprogress.topic.dto.CreateTopicRequest;
import com.example.codeprogress.topic.dto.TopicResponse;
import com.example.codeprogress.topic.dto.UpdateTopicRequest;
import com.example.codeprogress.topic.dto.UpdateTopicStatusRequest;
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
class TopicServiceTest {

    @Mock
    private TopicRepository topicRepository;

    private TopicService topicService;

    @BeforeEach
    void setUp() {
        topicService = new TopicService(topicRepository);
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
        verify(topicRepository).save(any(Topic.class));
    }

    @Test
    void returnsAllTopicsAsResponses() {
        Topic first = topic(1L, "Java", null, TopicStatus.PLANNED);
        Topic second = topic(2L, "SQL", "PostgreSQL", TopicStatus.IN_PROGRESS);
        when(topicRepository.findAll()).thenReturn(List.of(first, second));

        List<TopicResponse> responses = topicService.getAll();

        assertThat(responses).extracting(TopicResponse::id).containsExactly(1L, 2L);
        assertThat(responses).extracting(TopicResponse::title).containsExactly("Java", "SQL");
    }

    @Test
    void returnsTopicById() {
        when(topicRepository.findById(1L))
                .thenReturn(Optional.of(topic(1L, "Java", "Core Java", TopicStatus.PLANNED)));

        TopicResponse response = topicService.getById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("Java");
    }

    @Test
    void throwsWhenGettingUnknownTopic() {
        when(topicRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> topicService.getById(99L))
                .isInstanceOf(TopicNotFoundException.class)
                .hasMessage("Topic with id 99 was not found");
    }

    @Test
    void updatesTopicDetailsWithoutChangingStatus() {
        Topic topic = topic(1L, "Old title", "Old description", TopicStatus.IN_PROGRESS);
        LocalDateTime previousUpdatedAt = topic.getUpdatedAt();
        when(topicRepository.findById(1L)).thenReturn(Optional.of(topic));

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
        when(topicRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> topicService.update(99L, new UpdateTopicRequest("Java", null)))
                .isInstanceOf(TopicNotFoundException.class);
    }

    @Test
    void updatesOnlyTopicStatus() {
        Topic topic = topic(1L, "Java", "Core Java", TopicStatus.PLANNED);
        LocalDateTime previousUpdatedAt = topic.getUpdatedAt();
        when(topicRepository.findById(1L)).thenReturn(Optional.of(topic));

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
        when(topicRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> topicService.updateStatus(
                99L,
                new UpdateTopicStatusRequest(TopicStatus.COMPLETED)
        )).isInstanceOf(TopicNotFoundException.class);
    }

    @Test
    void deletesExistingTopic() {
        Topic topic = topic(1L, "Java", null, TopicStatus.PLANNED);
        when(topicRepository.findById(1L)).thenReturn(Optional.of(topic));

        topicService.delete(1L);

        verify(topicRepository).delete(topic);
    }

    @Test
    void throwsWhenDeletingUnknownTopic() {
        when(topicRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> topicService.delete(99L))
                .isInstanceOf(TopicNotFoundException.class);
        verify(topicRepository, never()).delete(any(Topic.class));
    }

    private Topic topic(
            Long id,
            String title,
            String description,
            TopicStatus status
    ) {
        Topic topic = new Topic(title, description);
        if (status != TopicStatus.PLANNED) {
            topic.updateStatus(status);
        }
        ReflectionTestUtils.setField(topic, "id", id);
        ReflectionTestUtils.setField(topic, "createdAt", LocalDateTime.now().minusDays(2));
        ReflectionTestUtils.setField(topic, "updatedAt", LocalDateTime.now().minusDays(1));
        return topic;
    }
}
