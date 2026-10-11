package com.example.codeprogress.learningtask.repository;

import com.example.codeprogress.learningtask.LearningTask;
import com.example.codeprogress.learningtask.LearningTaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LearningTaskRepository extends JpaRepository<LearningTask, Long> {

    List<LearningTask> findAllByTopicId(Long topicId);

    long countByTopicId(Long topicId);

    long countByTopicIdAndStatus(Long topicId, LearningTaskStatus status);
}
