package com.example.codeprogress.analytics.repository;

import java.time.LocalDate;

public interface AnalyticsWeeklyActivity {

    LocalDate getWeekStart();
    Long getStudyMinutes();
    Long getStudySessions();
}
