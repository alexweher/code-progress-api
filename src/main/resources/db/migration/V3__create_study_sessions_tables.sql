CREATE TABLE study_sessions (
    id BIGSERIAL PRIMARY KEY,
    topic_id BIGINT NOT NULL,
    session_date DATE NOT NULL,
    duration_minutes INTEGER NOT NULL,
    note VARCHAR(2000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_study_sessions_topic
        FOREIGN KEY (topic_id) REFERENCES topics (id) ON DELETE CASCADE,
    CONSTRAINT chk_study_sessions_duration_positive
        CHECK (duration_minutes > 0)
);

CREATE TABLE study_session_tasks (
    study_session_id BIGINT NOT NULL,
    learning_task_id BIGINT NOT NULL,
    PRIMARY KEY (study_session_id, learning_task_id),
    CONSTRAINT fk_study_session_tasks_session
        FOREIGN KEY (study_session_id) REFERENCES study_sessions (id) ON DELETE CASCADE,
    CONSTRAINT fk_study_session_tasks_learning_task
        FOREIGN KEY (learning_task_id) REFERENCES learning_tasks (id) ON DELETE CASCADE
);
