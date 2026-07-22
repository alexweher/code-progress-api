package com.example.codeprogress.topic.service;

import com.example.codeprogress.topic.Topic;
import com.example.codeprogress.topic.dto.CreateTopicRequest;
import com.example.codeprogress.topic.dto.TopicResponse;
import com.example.codeprogress.topic.dto.UpdateTopicRequest;
import com.example.codeprogress.topic.dto.UpdateTopicStatusRequest;
import com.example.codeprogress.topic.exception.TopicNotFoundException;
import com.example.codeprogress.topic.repository.TopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TopicService {

    private final TopicRepository topicRepository;

    public TopicService(TopicRepository topicRepository) {
        this.topicRepository = topicRepository;
    }

    @Transactional
    public TopicResponse create(CreateTopicRequest request) {
        Topic topic = new Topic(request.title(), request.description());
        return toResponse(topicRepository.save(topic));
    }

    @Transactional(readOnly = true)
    public List<TopicResponse> getAll() {
        return topicRepository.findAll().stream()
                .map(TopicService::toResponse)
                .toList();
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
        topicRepository.delete(topic);
    }

    private Topic findById(Long id) {
        return topicRepository.findById(id)
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
