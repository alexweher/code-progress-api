package com.example.codeprogress.learningtask.service;

import com.example.codeprogress.common.pagination.InvalidPageRequestException;
import com.example.codeprogress.learningtask.LearningTask;
import com.example.codeprogress.learningtask.LearningTaskDifficulty;
import com.example.codeprogress.learningtask.LearningTaskPriority;
import com.example.codeprogress.learningtask.LearningTaskStatus;
import com.example.codeprogress.learningtask.dto.CreateLearningTaskRequest;
import com.example.codeprogress.learningtask.dto.LearningTaskResponse;
import com.example.codeprogress.learningtask.dto.UpdateLearningTaskRequest;
import com.example.codeprogress.learningtask.dto.UpdateLearningTaskPlanningRequest;
import com.example.codeprogress.learningtask.dto.UpdateLearningTaskStatusRequest;
import com.example.codeprogress.learningtask.exception.LearningTaskDeletionConflictException;
import com.example.codeprogress.learningtask.exception.LearningTaskNotFoundException;
import com.example.codeprogress.learningtask.repository.LearningTaskRepository;
import com.example.codeprogress.studysession.repository.StudySessionRepository;
import com.example.codeprogress.topic.Topic;
import com.example.codeprogress.topic.exception.TopicNotFoundException;
import com.example.codeprogress.topic.repository.TopicRepository;
import com.example.codeprogress.user.User;
import com.example.codeprogress.user.security.CurrentUserProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
public class LearningTaskService {

    private final LearningTaskRepository learningTaskRepository;
    private final TopicRepository topicRepository;
    private final StudySessionRepository studySessionRepository;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public LearningTaskService(
            LearningTaskRepository learningTaskRepository,
            TopicRepository topicRepository,
            StudySessionRepository studySessionRepository,
            CurrentUserProvider currentUserProvider,
            Clock clock
    ) {
        this.learningTaskRepository = learningTaskRepository;
        this.topicRepository = topicRepository;
        this.studySessionRepository = studySessionRepository;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    @Transactional
    public LearningTaskResponse create(Long topicId, CreateLearningTaskRequest request) {
        Topic topic = findTopicById(topicId);
        LearningTaskPriority priority = request.priority() == null
                ? LearningTaskPriority.MEDIUM
                : request.priority();
        LearningTask task = new LearningTask(
                topic,
                request.title(),
                request.description(),
                request.difficulty(),
                request.deadline(),
                priority
        );
        return toResponse(learningTaskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public List<LearningTaskResponse> getAllByTopicId(Long topicId) {
        User owner = currentUserProvider.getCurrentUser();
        findTopicById(topicId, owner.getId());
        return learningTaskRepository.findAllByTopicIdAndTopicOwnerId(topicId, owner.getId()).stream()
                .map(LearningTaskService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<LearningTaskResponse> search(
            Long topicId,
            LearningTaskStatus status,
            LearningTaskDifficulty difficulty,
            Pageable pageable
    ) {
        return search(topicId, status, difficulty, null, null, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<LearningTaskResponse> search(
            Long topicId,
            LearningTaskStatus status,
            LearningTaskDifficulty difficulty,
            LearningTaskPriority priority,
            LocalDate deadlineFrom,
            LocalDate deadlineTo,
            Pageable pageable
    ) {
        if (topicId != null && topicId < 1) {
            throw new InvalidPageRequestException("Topic id must be greater than zero");
        }
        if (deadlineFrom != null && deadlineTo != null && deadlineFrom.isAfter(deadlineTo)) {
            throw new InvalidPageRequestException(
                    "Deadline from must be earlier than or equal to deadline to"
            );
        }

        Specification<LearningTask> specification = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
        Long ownerId = currentUserProvider.getCurrentUser().getId();
        specification = specification.and(
                (root, query, criteriaBuilder) -> criteriaBuilder.equal(
                        root.get("topic").get("owner").get("id"), ownerId
                )
        );
        if (topicId != null) {
            specification = specification.and(
                    (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("topic").get("id"), topicId)
            );
        }
        if (status != null) {
            specification = specification.and(
                    (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"), status)
            );
        }
        if (difficulty != null) {
            specification = specification.and(
                    (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("difficulty"), difficulty)
            );
        }
        if (priority != null) {
            specification = specification.and(
                    (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("priority"), priority)
            );
        }
        if (deadlineFrom != null) {
            specification = specification.and(
                    (root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(
                            root.get("deadline"), deadlineFrom
                    )
            );
        }
        if (deadlineTo != null) {
            specification = specification.and(
                    (root, query, criteriaBuilder) -> criteriaBuilder.lessThanOrEqualTo(
                            root.get("deadline"), deadlineTo
                    )
            );
        }

        return learningTaskRepository.findAll(specification, pageable).map(LearningTaskService::toResponse);
    }

    @Transactional(readOnly = true)
    public LearningTaskResponse getById(Long id) {
        return toResponse(findTaskById(id));
    }

    @Transactional
    public LearningTaskResponse update(Long id, UpdateLearningTaskRequest request) {
        LearningTask task = findTaskById(id);
        task.updateDetails(request.title(), request.description(), request.difficulty());
        return toResponse(task);
    }

    @Transactional
    public LearningTaskResponse updateStatus(Long id, UpdateLearningTaskStatusRequest request) {
        LearningTask task = findTaskById(id);
        task.updateStatus(request.status());
        return toResponse(task);
    }

    @Transactional
    public LearningTaskResponse updatePlanning(Long id, UpdateLearningTaskPlanningRequest request) {
        LearningTask task = findTaskById(id);
        task.updatePlanning(request.deadline(), request.priority());
        return toResponse(task);
    }

    @Transactional(readOnly = true)
    public Page<LearningTaskResponse> getDueToday(Pageable pageable) {
        LocalDate today = LocalDate.now(clock);
        Long ownerId = currentUserProvider.getCurrentUser().getId();
        return learningTaskRepository.findAllByTopicOwnerIdAndStatusNotAndDeadline(
                ownerId,
                LearningTaskStatus.COMPLETED,
                today,
                pageable
        ).map(LearningTaskService::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<LearningTaskResponse> getUpcoming(Pageable pageable) {
        LocalDate today = LocalDate.now(clock);
        Long ownerId = currentUserProvider.getCurrentUser().getId();
        return learningTaskRepository.findAllByTopicOwnerIdAndStatusNotAndDeadlineBetween(
                ownerId,
                LearningTaskStatus.COMPLETED,
                today,
                today.plusDays(6),
                pageable
        ).map(LearningTaskService::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<LearningTaskResponse> getOverdue(Pageable pageable) {
        LocalDate today = LocalDate.now(clock);
        Long ownerId = currentUserProvider.getCurrentUser().getId();
        return learningTaskRepository.findAllByTopicOwnerIdAndStatusNotAndDeadlineBefore(
                ownerId,
                LearningTaskStatus.COMPLETED,
                today,
                pageable
        ).map(LearningTaskService::toResponse);
    }

    @Transactional
    public void delete(Long id) {
        LearningTask task = findTaskById(id);
        if (studySessionRepository.existsByCompletedTasks_IdAndTopicOwnerId(
                id,
                task.getTopic().getOwner().getId()
        )) {
            throw new LearningTaskDeletionConflictException(id);
        }
        learningTaskRepository.delete(task);
    }

    private Topic findTopicById(Long id) {
        return findTopicById(id, currentUserProvider.getCurrentUser().getId());
    }

    private Topic findTopicById(Long id, Long ownerId) {
        return topicRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new TopicNotFoundException(id));
    }

    private LearningTask findTaskById(Long id) {
        Long ownerId = currentUserProvider.getCurrentUser().getId();
        return learningTaskRepository.findByIdAndTopicOwnerId(id, ownerId)
                .orElseThrow(() -> new LearningTaskNotFoundException(id));
    }

    private static LearningTaskResponse toResponse(LearningTask task) {
        return new LearningTaskResponse(
                task.getId(),
                task.getTopic().getId(),
                task.getTitle(),
                task.getDescription(),
                task.getDifficulty(),
                task.getStatus(),
                task.getDeadline(),
                task.getPriority(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
