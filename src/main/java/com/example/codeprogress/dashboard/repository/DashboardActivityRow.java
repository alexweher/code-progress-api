package com.example.codeprogress.dashboard.repository;

import java.time.LocalDate;

public interface DashboardActivityRow {

    LocalDate getActivityDate();

    Long getStudyMinutes();

    Long getStudySessionsCount();
}
