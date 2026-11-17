package com.example.codeprogress.analytics.repository;

import java.time.LocalDate;

public interface AnalyticsTopicRow {

    Long getTopicId();
    String getTitle();
    String getStatus();
    Long getTotalTasks();
    Long getPlannedTasks();
    Long getActiveTasks();
    Long getCompletedTasks();
    Long getStudyMinutes();
    Long getStudySessions();
    Long getActiveDays();
    LocalDate getLastStudyDate();
}
