package com.example.codeprogress.studysession.service;

import com.example.codeprogress.common.pagination.InvalidPageRequestException;
import com.example.codeprogress.learningtask.LearningTask;
import com.example.codeprogress.learningtask.exception.LearningTaskNotFoundException;
import com.example.codeprogress.learningtask.repository.LearningTaskRepository;
import com.example.codeprogress.studysession.StudySession;
import com.example.codeprogress.studysession.dto.CreateStudySessionRequest;
import com.example.codeprogress.studysession.dto.StudySessionResponse;
import com.example.codeprogress.studysession.dto.UpdateStudySessionRequest;
import com.example.codeprogress.studysession.exception.LearningTaskDoesNotBelongToTopicException;
import com.example.codeprogress.studysession.exception.StudySessionNotFoundException;
import com.example.codeprogress.studysession.repository.StudySessionRepository;
import com.example.codeprogress.topic.Topic;
import com.example.codeprogress.topic.exception.TopicNotFoundException;
import com.example.codeprogress.topic.repository.TopicRepository;
import com.example.codeprogress.user.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.LinkedHashSet;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StudySessionService {

    private final StudySessionRepository studySessionRepository;
    private final TopicRepository topicRepository;
    private final LearningTaskRepository learningTaskRepository;
    private final CurrentUserProvider currentUserProvider;

    public StudySessionService(
            StudySessionRepository studySessionRepository,
            TopicRepository topicRepository,
            LearningTaskRepository learningTaskRepository,
            CurrentUserProvider currentUserProvider
    ) {
        this.studySessionRepository = studySessionRepository;
        this.topicRepository = topicRepository;
        this.learningTaskRepository = learningTaskRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public StudySessionResponse create(Long topicId, CreateStudySessionRequest request) {
        Long ownerId = currentUserProvider.getCurrentUser().getId();
        Topic topic = findTopicById(topicId, ownerId);
        Set<LearningTask> completedTasks = findCompletedTasks(topic, request.completedTaskIds(), ownerId);
        markTasksCompleted(completedTasks);

        StudySession session = new StudySession(
                topic,
                request.sessionDate(),
                request.durationMinutes(),
                request.note(),
                completedTasks
        );
        return toResponse(studySessionRepository.save(session));
    }

    @Transactional(readOnly = true)
    public List<StudySessionResponse> getAllByTopicId(Long topicId) {
        Long ownerId = currentUserProvider.getCurrentUser().getId();
        if (!topicRepository.existsByIdAndOwnerId(topicId, ownerId)) {
            throw new TopicNotFoundException(topicId);
        }
        return studySessionRepository.findAllByTopicIdAndTopicOwnerIdOrderBySessionDateDesc(
                        topicId,
                        ownerId
                ).stream()
                .map(StudySessionService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public StudySessionResponse getById(Long id) {
        Long ownerId = currentUserProvider.getCurrentUser().getId();
        return toResponse(findSessionById(id, ownerId));
    }

    @Transactional(readOnly = true)
    public Page<StudySessionResponse> search(
            Long topicId,
            LocalDate dateFrom,
            LocalDate dateTo,
            Pageable pageable
    ) {
        if (topicId != null && topicId < 1) {
            throw new InvalidPageRequestException("Topic id must be greater than zero");
        }
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new InvalidPageRequestException(
                    "Date from must be earlier than or equal to date to"
            );
        }

        Specification<StudySession> specification = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
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
        if (dateFrom != null) {
            specification = specification.and(
                    (root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(
                            root.get("sessionDate"), dateFrom
                    )
            );
        }
        if (dateTo != null) {
            specification = specification.and(
                    (root, query, criteriaBuilder) -> criteriaBuilder.lessThanOrEqualTo(
                            root.get("sessionDate"), dateTo
                    )
            );
        }

        return studySessionRepository.findAll(specification, pageable).map(StudySessionService::toResponse);
    }

    @Transactional
    public StudySessionResponse update(Long id, UpdateStudySessionRequest request) {
        Long ownerId = currentUserProvider.getCurrentUser().getId();
        StudySession session = findSessionById(id, ownerId);
        Set<LearningTask> previouslyCompletedTasks = session.getCompletedTasks();
        Set<LearningTask> completedTasks = findCompletedTasks(
                session.getTopic(),
                request.completedTaskIds(),
                ownerId
        );
        markTasksCompleted(completedTasks);
        session.updateDetails(request.sessionDate(), request.durationMinutes(), request.note(), completedTasks);
        restoreRemovedTasksIfNoOtherSessionCompletesThem(
                id,
                previouslyCompletedTasks,
                completedTasks,
                ownerId
        );
        return toResponse(session);
    }

    @Transactional
    public void delete(Long id) {
        Long ownerId = currentUserProvider.getCurrentUser().getId();
        StudySession session = findSessionById(id, ownerId);
        Set<LearningTask> completedTasks = session.getCompletedTasks();
        studySessionRepository.delete(session);
        restoreRemovedTasksIfNoOtherSessionCompletesThem(id, completedTasks, Set.of(), ownerId);
    }

    private Topic findTopicById(Long id, Long ownerId) {
        return topicRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new TopicNotFoundException(id));
    }

    private StudySession findSessionById(Long id, Long ownerId) {
        return studySessionRepository.findByIdAndTopicOwnerId(id, ownerId)
                .orElseThrow(() -> new StudySessionNotFoundException(id));
    }

    private Set<LearningTask> findCompletedTasks(Topic topic, Set<Long> taskIds, Long ownerId) {
        if (taskIds.isEmpty()) {
            return Set.of();
        }

        Map<Long, LearningTask> tasksById = learningTaskRepository
                .findAllByIdInAndTopicOwnerId(taskIds, ownerId)
                .stream()
                .collect(Collectors.toMap(LearningTask::getId, Function.identity()));
        Set<LearningTask> tasks = new LinkedHashSet<>();
        for (Long taskId : taskIds) {
            LearningTask task = tasksById.get(taskId);
            if (task == null) {
                throw new LearningTaskNotFoundException(taskId);
            }
            if (!task.getTopic().getId().equals(topic.getId())) {
                throw new LearningTaskDoesNotBelongToTopicException(taskId, topic.getId());
            }
            tasks.add(task);
        }
        return tasks;
    }

    private void markTasksCompleted(Set<LearningTask> tasks) {
        tasks.forEach(LearningTask::completeFromStudySession);
    }

    private void restoreRemovedTasksIfNoOtherSessionCompletesThem(
            Long sessionId,
            Set<LearningTask> previousTasks,
            Set<LearningTask> currentTasks,
            Long ownerId
    ) {
        Set<LearningTask> tasksToRestore = previousTasks.stream()
                .filter(task -> !currentTasks.contains(task))
                .filter(LearningTask::isStatusManagedByStudySession)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (tasksToRestore.isEmpty()) {
            return;
        }

        Set<Long> taskIds = tasksToRestore.stream()
                .map(LearningTask::getId)
                .collect(Collectors.toSet());
        Set<Long> taskIdsUsedByOtherSessions = studySessionRepository.findTaskIdsUsedByOtherSessions(
                taskIds,
                sessionId,
                ownerId
        );

        tasksToRestore.stream()
                .filter(task -> !taskIdsUsedByOtherSessions.contains(task.getId()))
                .forEach(LearningTask::restoreStatusBeforeStudySessionCompletion);
    }

    private static StudySessionResponse toResponse(StudySession session) {
        Set<Long> completedTaskIds = session.getCompletedTasks().stream()
                .map(LearningTask::getId)
                .collect(Collectors.toUnmodifiableSet());

        return new StudySessionResponse(
                session.getId(),
                session.getTopic().getId(),
                session.getSessionDate(),
                session.getDurationMinutes(),
                session.getNote(),
                completedTaskIds,
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }
}
