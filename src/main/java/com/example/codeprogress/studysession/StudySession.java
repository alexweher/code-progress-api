package com.example.codeprogress.studysession;

import com.example.codeprogress.learningtask.LearningTask;
import com.example.codeprogress.topic.Topic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "study_sessions")
public class StudySession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(length = 2000)
    private String note;

    @ManyToMany
    @JoinTable(
            name = "study_session_tasks",
            joinColumns = @JoinColumn(name = "study_session_id"),
            inverseJoinColumns = @JoinColumn(name = "learning_task_id")
    )
    private Set<LearningTask> completedTasks = new LinkedHashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected StudySession() {
    }

    public StudySession(
            Topic topic,
            LocalDate sessionDate,
            int durationMinutes,
            String note,
            Set<LearningTask> completedTasks
    ) {
        this.topic = Objects.requireNonNull(topic, "topic must not be null");
        this.sessionDate = Objects.requireNonNull(sessionDate, "sessionDate must not be null");
        this.durationMinutes = durationMinutes;
        this.note = note;
        this.completedTasks = new LinkedHashSet<>(completedTasks);

        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void updateDetails(
            LocalDate sessionDate,
            int durationMinutes,
            String note,
            Set<LearningTask> completedTasks
    ) {
        this.sessionDate = Objects.requireNonNull(sessionDate, "sessionDate must not be null");
        this.durationMinutes = durationMinutes;
        this.note = note;
        this.completedTasks = new LinkedHashSet<>(completedTasks);
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
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
    public LocalDate getSessionDate() { return sessionDate; }
    public int getDurationMinutes() { return durationMinutes; }
    public String getNote() { return note; }
    public Set<LearningTask> getCompletedTasks() { return Set.copyOf(completedTasks); }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
