package com.example.codeprogress.studysession.exception;

public class LearningTaskDoesNotBelongToTopicException extends RuntimeException {

    public LearningTaskDoesNotBelongToTopicException(Long taskId, Long topicId) {
        super("Learning task with id " + taskId + " does not belong to topic with id " + topicId);
    }
}
