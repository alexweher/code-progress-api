package com.example.codeprogress.studysession.repository;

import java.time.LocalDate;

public interface StudySessionStatistics {

    Long getTotalStudyMinutes();

    Long getStudySessionsCount();

    LocalDate getLastStudyDate();
}
