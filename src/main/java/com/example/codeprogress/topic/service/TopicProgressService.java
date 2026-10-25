package com.example.codeprogress.topic.service;

import com.example.codeprogress.learningtask.LearningTaskStatus;
import com.example.codeprogress.learningtask.repository.LearningTaskRepository;
import com.example.codeprogress.studysession.repository.StudySessionRepository;
import com.example.codeprogress.studysession.repository.StudySessionStatistics;
import com.example.codeprogress.topic.dto.TopicProgressResponse;
import com.example.codeprogress.topic.exception.TopicNotFoundException;
import com.example.codeprogress.topic.repository.TopicRepository;
import com.example.codeprogress.user.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TopicProgressService {

    private final TopicRepository topicRepository;
    private final LearningTaskRepository learningTaskRepository;
    private final StudySessionRepository studySessionRepository;
    private final CurrentUserProvider currentUserProvider;

    public TopicProgressService(
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

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public TopicProgressResponse getProgress(Long topicId) {
        Long ownerId = currentUserProvider.getCurrentUser().getId();
        if (!topicRepository.existsByIdAndOwnerId(topicId, ownerId)) {
            throw new TopicNotFoundException(topicId);
        }

        long totalTasks = learningTaskRepository.countByTopicIdAndTopicOwnerId(topicId, ownerId);
        long completedTasks = learningTaskRepository.countByTopicIdAndTopicOwnerIdAndStatus(
                topicId,
                ownerId,
                LearningTaskStatus.COMPLETED
        );
        StudySessionStatistics sessionStatistics = studySessionRepository.getStatisticsByTopicIdAndOwnerId(
                topicId,
                ownerId
        );
        int progressPercent = totalTasks == 0
                ? 0
                : (int) Math.round(completedTasks * 100.0 / totalTasks);

        return new TopicProgressResponse(
                topicId,
                totalTasks,
                completedTasks,
                progressPercent,
                sessionStatistics.getTotalStudyMinutes(),
                sessionStatistics.getStudySessionsCount(),
                sessionStatistics.getLastStudyDate()
        );
    }
}
