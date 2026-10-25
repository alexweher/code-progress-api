package com.example.codeprogress.dashboard.repository;

import java.time.LocalDate;

public interface DashboardStudyStatistics {

    Long getTotalStudyMinutes();

    Long getStudyMinutesLast7Days();

    Long getStudySessionsCount();

    LocalDate getLastStudyDate();
}
