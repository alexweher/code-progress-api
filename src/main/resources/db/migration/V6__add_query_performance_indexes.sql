CREATE INDEX idx_learning_tasks_topic_status
    ON learning_tasks (topic_id, status);

CREATE INDEX idx_study_sessions_topic_date
    ON study_sessions (topic_id, session_date DESC)
    INCLUDE (duration_minutes);

CREATE INDEX idx_study_session_tasks_learning_task
    ON study_session_tasks (learning_task_id);
