package com.example.codeprogress.learningtask.service;

import com.example.codeprogress.learningtask.LearningTask;
import com.example.codeprogress.learningtask.dto.CreateLearningTaskRequest;
import com.example.codeprogress.learningtask.dto.LearningTaskResponse;
import com.example.codeprogress.learningtask.dto.UpdateLearningTaskRequest;
import com.example.codeprogress.learningtask.dto.UpdateLearningTaskStatusRequest;
import com.example.codeprogress.learningtask.exception.LearningTaskNotFoundException;
import com.example.codeprogress.learningtask.repository.LearningTaskRepository;
import com.example.codeprogress.topic.Topic;
import com.example.codeprogress.topic.exception.TopicNotFoundException;
import com.example.codeprogress.topic.repository.TopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LearningTaskService {

    private final LearningTaskRepository learningTaskRepository;
    private final TopicRepository topicRepository;

    public LearningTaskService(LearningTaskRepository learningTaskRepository, TopicRepository topicRepository) {
        this.learningTaskRepository = learningTaskRepository;
        this.topicRepository = topicRepository;
    }

    @Transactional
    public LearningTaskResponse create(Long topicId, CreateLearningTaskRequest request) {
        Topic topic = findTopicById(topicId);
        LearningTask task = new LearningTask(topic, request.title(), request.description(), request.difficulty());
        return toResponse(learningTaskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public List<LearningTaskResponse> getAllByTopicId(Long topicId) {
        findTopicById(topicId);
        return learningTaskRepository.findAllByTopicId(topicId).stream()
                .map(LearningTaskService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public LearningTaskResponse getById(Long id) {
        return toResponse(findTaskById(id));
    }

    @Transactional
    public LearningTaskResponse update(Long id, UpdateLearningTaskRequest request) {
        LearningTask task = findTaskById(id);
        task.updateDetails(request.title(), request.description(), request.difficulty());
        return toResponse(task);
    }

    @Transactional
    public LearningTaskResponse updateStatus(Long id, UpdateLearningTaskStatusRequest request) {
        LearningTask task = findTaskById(id);
        task.updateStatus(request.status());
        return toResponse(task);
    }

    @Transactional
    public void delete(Long id) {
        learningTaskRepository.delete(findTaskById(id));
    }

    private Topic findTopicById(Long id) {
        return topicRepository.findById(id)
                .orElseThrow(() -> new TopicNotFoundException(id));
    }

    private LearningTask findTaskById(Long id) {
        return learningTaskRepository.findById(id)
                .orElseThrow(() -> new LearningTaskNotFoundException(id));
    }

    private static LearningTaskResponse toResponse(LearningTask task) {
        return new LearningTaskResponse(
                task.getId(),
                task.getTopic().getId(),
                task.getTitle(),
                task.getDescription(),
                task.getDifficulty(),
                task.getStatus(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
