package com.example.codeprogress.studysession.service;

import com.example.codeprogress.learningtask.LearningTask;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class StudySessionService {

    private final StudySessionRepository studySessionRepository;
    private final TopicRepository topicRepository;
    private final LearningTaskRepository learningTaskRepository;

    public StudySessionService(
            StudySessionRepository studySessionRepository,
            TopicRepository topicRepository,
            LearningTaskRepository learningTaskRepository
    ) {
        this.studySessionRepository = studySessionRepository;
        this.topicRepository = topicRepository;
        this.learningTaskRepository = learningTaskRepository;
    }

    @Transactional
    public StudySessionResponse create(Long topicId, CreateStudySessionRequest request) {
        Topic topic = findTopicById(topicId);
        Set<LearningTask> completedTasks = findCompletedTasks(topic, request.completedTaskIds());
        markTasksCompleted(completedTasks);

        StudySession session = new StudySession(
                topic,
                request.sessionDate(),
                request.durationMinutes(),
                request.note(),
                completedTasks
        );
        return toResponse(studySessionRepository.save(session));
    }

    @Transactional(readOnly = true)
    public List<StudySessionResponse> getAllByTopicId(Long topicId) {
        findTopicById(topicId);
        return studySessionRepository.findAllByTopicIdOrderBySessionDateDesc(topicId).stream()
                .map(StudySessionService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public StudySessionResponse getById(Long id) {
        return toResponse(findSessionById(id));
    }

    @Transactional
    public StudySessionResponse update(Long id, UpdateStudySessionRequest request) {
        StudySession session = findSessionById(id);
        Set<LearningTask> completedTasks = findCompletedTasks(session.getTopic(), request.completedTaskIds());
        markTasksCompleted(completedTasks);
        session.updateDetails(request.sessionDate(), request.durationMinutes(), request.note(), completedTasks);
        return toResponse(session);
    }

    @Transactional
    public void delete(Long id) {
        studySessionRepository.delete(findSessionById(id));
    }

    private Topic findTopicById(Long id) {
        return topicRepository.findById(id)
                .orElseThrow(() -> new TopicNotFoundException(id));
    }

    private StudySession findSessionById(Long id) {
        return studySessionRepository.findById(id)
                .orElseThrow(() -> new StudySessionNotFoundException(id));
    }

    private Set<LearningTask> findCompletedTasks(Topic topic, Set<Long> taskIds) {
        Set<LearningTask> tasks = new LinkedHashSet<>();
        for (Long taskId : taskIds) {
            LearningTask task = learningTaskRepository.findById(taskId)
                    .orElseThrow(() -> new LearningTaskNotFoundException(taskId));
            if (!task.getTopic().getId().equals(topic.getId())) {
                throw new LearningTaskDoesNotBelongToTopicException(taskId, topic.getId());
            }
            tasks.add(task);
        }
        return tasks;
    }

    private void markTasksCompleted(Set<LearningTask> tasks) {
        tasks.forEach(task -> task.updateStatus(LearningTaskStatus.COMPLETED));
    }

    private static StudySessionResponse toResponse(StudySession session) {
        Set<Long> completedTaskIds = session.getCompletedTasks().stream()
                .map(LearningTask::getId)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());

        return new StudySessionResponse(
                session.getId(),
                session.getTopic().getId(),
                session.getSessionDate(),
                session.getDurationMinutes(),
                session.getNote(),
                completedTaskIds,
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }
}
