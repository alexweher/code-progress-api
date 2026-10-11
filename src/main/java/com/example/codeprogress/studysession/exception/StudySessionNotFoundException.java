package com.example.codeprogress.studysession.exception;

public class StudySessionNotFoundException extends RuntimeException {

    public StudySessionNotFoundException(Long id) {
        super("Study session with id " + id + " was not found");
    }
}
