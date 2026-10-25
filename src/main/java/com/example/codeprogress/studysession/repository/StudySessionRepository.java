package com.example.codeprogress.studysession.repository;

import com.example.codeprogress.studysession.StudySession;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface StudySessionRepository extends JpaRepository<StudySession, Long>, JpaSpecificationExecutor<StudySession> {

    @EntityGraph(attributePaths = "completedTasks")
    List<StudySession> findAllByTopicIdAndTopicOwnerIdOrderBySessionDateDesc(Long topicId, Long ownerId);

    @EntityGraph(attributePaths = "completedTasks")
    Optional<StudySession> findByIdAndTopicOwnerId(Long id, Long ownerId);

    boolean existsByTopicIdAndTopicOwnerId(Long topicId, Long ownerId);

    boolean existsByCompletedTasks_IdAndTopicOwnerId(Long taskId, Long ownerId);

    @Query("""
            select distinct task.id
            from StudySession session
            join session.completedTasks task
            where task.id in :taskIds
              and session.id <> :sessionId
              and session.topic.owner.id = :ownerId
            """)
    Set<Long> findTaskIdsUsedByOtherSessions(
            @Param("taskIds") Set<Long> taskIds,
            @Param("sessionId") Long sessionId,
            @Param("ownerId") Long ownerId
    );

    @Query("""
            select coalesce(sum(session.durationMinutes), 0) as totalStudyMinutes,
                   count(session) as studySessionsCount,
                   max(session.sessionDate) as lastStudyDate
            from StudySession session
            where session.topic.id = :topicId
              and session.topic.owner.id = :ownerId
            """)
    StudySessionStatistics getStatisticsByTopicIdAndOwnerId(
            @Param("topicId") Long topicId,
            @Param("ownerId") Long ownerId
    );
}
