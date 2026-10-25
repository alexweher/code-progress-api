package com.example.codeprogress.topic.exception;

public class TopicDeletionConflictException extends RuntimeException {

    public TopicDeletionConflictException(Long topicId) {
        super("Topic with id " + topicId + " cannot be deleted because it has learning tasks or study sessions");
    }
}
