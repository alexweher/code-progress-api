package com.example.codeprogress.learningtask.exception;

public class LearningTaskNotFoundException extends RuntimeException {

    public LearningTaskNotFoundException(Long id) {
        super("Learning task with id " + id + " was not found");
    }
}
