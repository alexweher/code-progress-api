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

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected LearningTask() {
    }

    public LearningTask(Topic topic, String title, String description, LearningTaskDifficulty difficulty) {
        this.topic = Objects.requireNonNull(topic, "topic must not be null");
        this.title = Objects.requireNonNull(title, "title must not be null");
        this.description = description;
        this.difficulty = Objects.requireNonNull(difficulty, "difficulty must not be null");
        this.status = LearningTaskStatus.PLANNED;

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
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (status == null) {
            status = LearningTaskStatus.PLANNED;
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
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
