package com.example.codeprogress.analytics.repository;

import java.time.LocalDate;

public interface AnalyticsStudyStatistics {

    Long getAllTimeStudyMinutes();
    Long getAllTimeStudySessions();
    Long getAllTimeActiveDays();
    LocalDate getLastStudyDate();
    Long getPeriodStudyMinutes();
    Long getPeriodStudySessions();
    Long getPeriodActiveDays();
}
