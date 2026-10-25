package com.example.codeprogress.topic.service;

import com.example.codeprogress.learningtask.repository.LearningTaskRepository;
import com.example.codeprogress.studysession.repository.StudySessionRepository;
import com.example.codeprogress.topic.Topic;
import com.example.codeprogress.topic.dto.CreateTopicRequest;
import com.example.codeprogress.topic.dto.TopicResponse;
import com.example.codeprogress.topic.dto.UpdateTopicRequest;
import com.example.codeprogress.topic.dto.UpdateTopicStatusRequest;
import com.example.codeprogress.topic.exception.TopicDeletionConflictException;
import com.example.codeprogress.topic.exception.TopicNotFoundException;
import com.example.codeprogress.topic.repository.TopicRepository;
import com.example.codeprogress.user.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Service
public class TopicService {

    private final TopicRepository topicRepository;
    private final LearningTaskRepository learningTaskRepository;
    private final StudySessionRepository studySessionRepository;
    private final CurrentUserProvider currentUserProvider;

    public TopicService(
            TopicRepository topicRepository,
            LearningTaskRepository learningTaskRepository,
            StudySessionRepository studySessionRepository,
            CurrentUserProvider currentUserProvider
    ) {
        this.topicRepository = topicRepository;
        this.learningTaskRepository = learningTaskRepository;
        this.studySessionRepository = studySessionRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public TopicResponse create(CreateTopicRequest request) {
        Topic topic = new Topic(currentUserProvider.getCurrentUser(), request.title(), request.description());
        return toResponse(topicRepository.save(topic));
    }

    @Transactional(readOnly = true)
    public List<TopicResponse> getAll() {
        Long ownerId = currentUserProvider.getCurrentUser().getId();
        return topicRepository.findAllByOwnerId(ownerId).stream()
                .map(TopicService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<TopicResponse> search(Pageable pageable) {
        Long ownerId = currentUserProvider.getCurrentUser().getId();
        return topicRepository.findAllByOwnerId(ownerId, pageable).map(TopicService::toResponse);
    }

    @Transactional(readOnly = true)
    public TopicResponse getById(Long id) {
        return toResponse(findById(id));
    }

    @Transactional
    public TopicResponse update(Long id, UpdateTopicRequest request) {
        Topic topic = findById(id);
        topic.updateDetails(request.title(), request.description());
        return toResponse(topic);
    }

    @Transactional
    public TopicResponse updateStatus(Long id, UpdateTopicStatusRequest request) {
        Topic topic = findById(id);
        topic.updateStatus(request.status());
        return toResponse(topic);
    }

    @Transactional
    public void delete(Long id) {
        Topic topic = findById(id);
        Long ownerId = topic.getOwner().getId();
        if (learningTaskRepository.existsByTopicIdAndTopicOwnerId(id, ownerId)
                || studySessionRepository.existsByTopicIdAndTopicOwnerId(id, ownerId)) {
            throw new TopicDeletionConflictException(id);
        }
        topicRepository.delete(topic);
    }

    private Topic findById(Long id) {
        Long ownerId = currentUserProvider.getCurrentUser().getId();
        return topicRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new TopicNotFoundException(id));
    }

    private static TopicResponse toResponse(Topic topic) {
        return new TopicResponse(
                topic.getId(),
                topic.getTitle(),
                topic.getDescription(),
                topic.getStatus(),
                topic.getCreatedAt(),
                topic.getUpdatedAt()
        );
    }
}
