package com.example.codeprogress.analytics.repository;

import com.example.codeprogress.studysession.StudySession;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AnalyticsRepository extends Repository<StudySession, Long> {

    @Query("""
            select coalesce(sum(session.durationMinutes), 0) as allTimeStudyMinutes,
                   count(session) as allTimeStudySessions,
                   count(distinct session.sessionDate) as allTimeActiveDays,
                   max(session.sessionDate) as lastStudyDate,
                   coalesce(sum(case when session.sessionDate between :from and :to
                       then session.durationMinutes else 0 end), 0) as periodStudyMinutes,
                   coalesce(sum(case when session.sessionDate between :from and :to
                       then 1 else 0 end), 0) as periodStudySessions,
                   count(distinct case when session.sessionDate between :from and :to
                       then session.sessionDate else null end) as periodActiveDays
            from StudySession session
            where session.topic.owner.id = :ownerId
            """)
    AnalyticsStudyStatistics getStudyStatistics(
            @Param("ownerId") Long ownerId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    @Query(value = """
            with weekly_activity as (
                select cast(date_trunc('week', cast(session.session_date as timestamp)) as date) as week_start,
                       sum(session.duration_minutes) as study_minutes,
                       count(*) as study_sessions
                from study_sessions session
                join topics topic on topic.id = session.topic_id
                where topic.owner_user_id = :ownerId
                  and session.session_date between cast(:dateFrom as date) and cast(:dateTo as date)
                group by 1
            )
            select cast(weeks.week_start as date) as "weekStart",
                   coalesce(activity.study_minutes, 0) as "studyMinutes",
                   coalesce(activity.study_sessions, 0) as "studySessions"
            from generate_series(
                date_trunc('week', cast(cast(:dateFrom as date) as timestamp)),
                date_trunc('week', cast(cast(:dateTo as date) as timestamp)),
                interval '1 week'
            ) as weeks(week_start)
            left join weekly_activity activity on activity.week_start = cast(weeks.week_start as date)
            order by weeks.week_start
            """, nativeQuery = true)
    List<AnalyticsWeeklyActivity> getWeeklyActivity(
            @Param("ownerId") Long ownerId,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo
    );

    @Query(value = """
            with owned_topics as (
                select id, title, status
                from topics
                where owner_user_id = :ownerId
            ), task_totals as (
                select task.topic_id,
                       count(*) as total_tasks,
                       count(*) filter (where task.status = 'PLANNED') as planned_tasks,
                       count(*) filter (where task.status = 'IN_PROGRESS') as active_tasks,
                       count(*) filter (where task.status = 'COMPLETED') as completed_tasks
                from learning_tasks task
                join owned_topics topic on topic.id = task.topic_id
                group by task.topic_id
            ), session_totals as (
                select session.topic_id,
                       sum(session.duration_minutes) as study_minutes,
                       count(*) as study_sessions,
                       count(distinct session.session_date) as active_days,
                       max(session.session_date) as last_study_date
                from study_sessions session
                join owned_topics topic on topic.id = session.topic_id
                where session.session_date between cast(:dateFrom as date) and cast(:dateTo as date)
                group by session.topic_id
            )
            select topic.id as "topicId", topic.title as "title", topic.status as "status",
                   coalesce(tasks.total_tasks, 0) as "totalTasks",
                   coalesce(tasks.planned_tasks, 0) as "plannedTasks",
                   coalesce(tasks.active_tasks, 0) as "activeTasks",
                   coalesce(tasks.completed_tasks, 0) as "completedTasks",
                   coalesce(sessions.study_minutes, 0) as "studyMinutes",
                   coalesce(sessions.study_sessions, 0) as "studySessions",
                   coalesce(sessions.active_days, 0) as "activeDays",
                   sessions.last_study_date as "lastStudyDate"
            from owned_topics topic
            left join task_totals tasks on tasks.topic_id = topic.id
            left join session_totals sessions on sessions.topic_id = topic.id
            order by topic.title asc, topic.id asc
            """, nativeQuery = true)
    List<AnalyticsTopicRow> getTopicStatistics(
            @Param("ownerId") Long ownerId,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo
    );
}
