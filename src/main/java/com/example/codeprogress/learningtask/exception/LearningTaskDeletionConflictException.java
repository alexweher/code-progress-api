package com.example.codeprogress.learningtask.exception;

public class LearningTaskDeletionConflictException extends RuntimeException {

    public LearningTaskDeletionConflictException(Long taskId) {
        super("Learning task with id " + taskId + " cannot be deleted because it is part of study session history");
    }
}
