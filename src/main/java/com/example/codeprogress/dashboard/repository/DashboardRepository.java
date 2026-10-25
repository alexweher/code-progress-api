package com.example.codeprogress.dashboard.repository;

import com.example.codeprogress.learningtask.LearningTaskStatus;
import com.example.codeprogress.topic.Topic;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface DashboardRepository extends Repository<Topic, Long> {

    @Query("select count(topic) from Topic topic where topic.owner.id = :ownerId")
    long countTopicsByOwnerId(@Param("ownerId") Long ownerId);

    @Query("""
            select count(task) as totalTasks,
                   coalesce(sum(case when task.status = :plannedStatus then 1 else 0 end), 0) as plannedTasks,
                   coalesce(sum(case when task.status = :activeStatus then 1 else 0 end), 0) as activeTasks,
                   coalesce(sum(case when task.status = :completedStatus then 1 else 0 end), 0) as completedTasks
            from LearningTask task
            where task.topic.owner.id = :ownerId
            """)
    DashboardTaskStatistics getTaskStatistics(
            @Param("ownerId") Long ownerId,
            @Param("plannedStatus") LearningTaskStatus plannedStatus,
            @Param("activeStatus") LearningTaskStatus activeStatus,
            @Param("completedStatus") LearningTaskStatus completedStatus
    );

    @Query("""
            select coalesce(sum(session.durationMinutes), 0) as totalStudyMinutes,
                   coalesce(sum(case
                       when session.sessionDate between :periodStart and :periodEnd
                       then session.durationMinutes else 0 end), 0) as studyMinutesLast7Days,
                   count(session) as studySessionsCount,
                   max(session.sessionDate) as lastStudyDate
            from StudySession session
            where session.topic.owner.id = :ownerId
              and session.sessionDate <= :periodEnd
            """)
    DashboardStudyStatistics getStudyStatistics(
            @Param("ownerId") Long ownerId,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd
    );

    @Query(value = """
            with daily_activity as (
                select session.session_date as activity_date,
                       sum(session.duration_minutes) as study_minutes,
                       count(*) as study_sessions_count
                from study_sessions session
                join topics topic on topic.id = session.topic_id
                where topic.owner_user_id = :ownerId
                  and session.session_date between cast(:dateFrom as date) and cast(:dateTo as date)
                group by session.session_date
            )
            select cast(days.activity_date as date) as "activityDate",
                   coalesce(activity.study_minutes, 0) as "studyMinutes",
                   coalesce(activity.study_sessions_count, 0) as "studySessionsCount"
            from generate_series(
                cast(:dateFrom as date),
                cast(:dateTo as date),
                interval '1 day'
            ) as days(activity_date)
            left join daily_activity activity on activity.activity_date = days.activity_date
            order by days.activity_date
            """, nativeQuery = true)
    List<DashboardActivityRow> getDailyActivity(
            @Param("ownerId") Long ownerId,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo
    );
}
