package com.example.codeprogress.dashboard.repository;

public interface DashboardTaskStatistics {

    Long getTotalTasks();

    Long getPlannedTasks();

    Long getActiveTasks();

    Long getCompletedTasks();
}
