package com.example.codeprogress.topic.service;

import com.example.codeprogress.learningtask.LearningTaskStatus;
import com.example.codeprogress.learningtask.repository.LearningTaskRepository;
import com.example.codeprogress.studysession.StudySession;
import com.example.codeprogress.studysession.repository.StudySessionRepository;
import com.example.codeprogress.topic.dto.TopicProgressResponse;
import com.example.codeprogress.topic.exception.TopicNotFoundException;
import com.example.codeprogress.topic.repository.TopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class TopicProgressService {

    private final TopicRepository topicRepository;
    private final LearningTaskRepository learningTaskRepository;
    private final StudySessionRepository studySessionRepository;

    public TopicProgressService(
            TopicRepository topicRepository,
            LearningTaskRepository learningTaskRepository,
            StudySessionRepository studySessionRepository
    ) {
        this.topicRepository = topicRepository;
        this.learningTaskRepository = learningTaskRepository;
        this.studySessionRepository = studySessionRepository;
    }

    @Transactional(readOnly = true)
    public TopicProgressResponse getProgress(Long topicId) {
        if (!topicRepository.existsById(topicId)) {
            throw new TopicNotFoundException(topicId);
        }

        long totalTasks = learningTaskRepository.countByTopicId(topicId);
        long completedTasks = learningTaskRepository.countByTopicIdAndStatus(topicId, LearningTaskStatus.COMPLETED);
        List<StudySession> sessions = studySessionRepository.findAllByTopicIdOrderBySessionDateDesc(topicId);
        long totalStudyMinutes = sessions.stream().mapToLong(StudySession::getDurationMinutes).sum();
        LocalDate lastStudyDate = sessions.stream()
                .map(StudySession::getSessionDate)
                .max(LocalDate::compareTo)
                .orElse(null);
        int progressPercent = totalTasks == 0
                ? 0
                : (int) Math.round(completedTasks * 100.0 / totalTasks);

        return new TopicProgressResponse(
                topicId,
                totalTasks,
                completedTasks,
                progressPercent,
                totalStudyMinutes,
                sessions.size(),
                lastStudyDate
        );
    }
}
