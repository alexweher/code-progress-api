package com.example.codeprogress.learningtask;

import com.example.codeprogress.topic.Topic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "learning_tasks")
public class LearningTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(length = 3000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private LearningTaskDifficulty difficulty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LearningTaskStatus status;

    @Column
    private LocalDate deadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private LearningTaskPriority priority;

    @Column(name = "status_managed_by_study_session", nullable = false)
    private boolean statusManagedByStudySession;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_before_study_session_completion", length = 20)
    private LearningTaskStatus statusBeforeStudySessionCompletion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected LearningTask() {
    }

    public LearningTask(Topic topic, String title, String description, LearningTaskDifficulty difficulty) {
        this(topic, title, description, difficulty, null, LearningTaskPriority.MEDIUM);
    }

    public LearningTask(
            Topic topic,
            String title,
            String description,
            LearningTaskDifficulty difficulty,
            LocalDate deadline,
            LearningTaskPriority priority
    ) {
        this.topic = Objects.requireNonNull(topic, "topic must not be null");
        this.title = Objects.requireNonNull(title, "title must not be null");
        this.description = description;
        this.difficulty = Objects.requireNonNull(difficulty, "difficulty must not be null");
        this.status = LearningTaskStatus.PLANNED;
        this.deadline = deadline;
        this.priority = Objects.requireNonNull(priority, "priority must not be null");
        this.statusManagedByStudySession = false;

        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void updateDetails(String title, String description, LearningTaskDifficulty difficulty) {
        this.title = Objects.requireNonNull(title, "title must not be null");
        this.description = description;
        this.difficulty = Objects.requireNonNull(difficulty, "difficulty must not be null");
        this.updatedAt = LocalDateTime.now();
    }

    public void updateStatus(LearningTaskStatus status) {
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.statusManagedByStudySession = false;
        this.statusBeforeStudySessionCompletion = null;
        this.updatedAt = LocalDateTime.now();
    }

    public void updatePlanning(LocalDate deadline, LearningTaskPriority priority) {
        this.deadline = deadline;
        this.priority = Objects.requireNonNull(priority, "priority must not be null");
        this.updatedAt = LocalDateTime.now();
    }

    public void completeFromStudySession() {
        if (status != LearningTaskStatus.COMPLETED) {
            statusBeforeStudySessionCompletion = status;
            status = LearningTaskStatus.COMPLETED;
            statusManagedByStudySession = true;
            updatedAt = LocalDateTime.now();
        }
    }

    public void restoreStatusBeforeStudySessionCompletion() {
        if (!statusManagedByStudySession) {
            return;
        }

        status = Objects.requireNonNull(
                statusBeforeStudySessionCompletion,
                "statusBeforeStudySessionCompletion must not be null when status is managed by a study session"
        );
        statusManagedByStudySession = false;
        statusBeforeStudySessionCompletion = null;
        updatedAt = LocalDateTime.now();
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (status == null) {
            status = LearningTaskStatus.PLANNED;
        }
        if (priority == null) {
            priority = LearningTaskPriority.MEDIUM;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Topic getTopic() { return topic; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public LearningTaskDifficulty getDifficulty() { return difficulty; }
    public LearningTaskStatus getStatus() { return status; }
    public LocalDate getDeadline() { return deadline; }
    public LearningTaskPriority getPriority() { return priority; }
    public boolean isStatusManagedByStudySession() { return statusManagedByStudySession; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
