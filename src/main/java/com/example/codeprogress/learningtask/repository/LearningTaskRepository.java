package com.example.codeprogress.learningtask.repository;

import com.example.codeprogress.learningtask.LearningTask;
import com.example.codeprogress.learningtask.LearningTaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface LearningTaskRepository extends JpaRepository<LearningTask, Long>, JpaSpecificationExecutor<LearningTask> {

    List<LearningTask> findAllByTopicIdAndTopicOwnerId(Long topicId, Long ownerId);

    Optional<LearningTask> findByIdAndTopicOwnerId(Long id, Long ownerId);

    List<LearningTask> findAllByIdInAndTopicOwnerId(Set<Long> ids, Long ownerId);

    boolean existsByTopicIdAndTopicOwnerId(Long topicId, Long ownerId);

    long countByTopicIdAndTopicOwnerId(Long topicId, Long ownerId);

    long countByTopicIdAndTopicOwnerIdAndStatus(Long topicId, Long ownerId, LearningTaskStatus status);

    Page<LearningTask> findAllByTopicOwnerIdAndStatusNotAndDeadline(
            Long ownerId,
            LearningTaskStatus excludedStatus,
            LocalDate deadline,
            Pageable pageable
    );

    Page<LearningTask> findAllByTopicOwnerIdAndStatusNotAndDeadlineBetween(
            Long ownerId,
            LearningTaskStatus excludedStatus,
            LocalDate deadlineFrom,
            LocalDate deadlineTo,
            Pageable pageable
    );

    Page<LearningTask> findAllByTopicOwnerIdAndStatusNotAndDeadlineBefore(
            Long ownerId,
            LearningTaskStatus excludedStatus,
            LocalDate deadline,
            Pageable pageable
    );
}
