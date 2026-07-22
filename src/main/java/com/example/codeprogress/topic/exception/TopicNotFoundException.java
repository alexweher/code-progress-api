package com.example.codeprogress.topic.exception;

public class TopicNotFoundException extends RuntimeException {

    public TopicNotFoundException(Long id) {
        super("Topic with id " + id + " was not found");
    }
}
