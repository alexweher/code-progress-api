ALTER TABLE learning_tasks
    DROP CONSTRAINT fk_learning_tasks_topic,
    ADD CONSTRAINT fk_learning_tasks_topic
        FOREIGN KEY (topic_id) REFERENCES topics (id);

ALTER TABLE study_sessions
    DROP CONSTRAINT fk_study_sessions_topic,
    ADD CONSTRAINT fk_study_sessions_topic
        FOREIGN KEY (topic_id) REFERENCES topics (id);

ALTER TABLE study_session_tasks
    DROP CONSTRAINT fk_study_session_tasks_learning_task,
    ADD CONSTRAINT fk_study_session_tasks_learning_task
        FOREIGN KEY (learning_task_id) REFERENCES learning_tasks (id);
