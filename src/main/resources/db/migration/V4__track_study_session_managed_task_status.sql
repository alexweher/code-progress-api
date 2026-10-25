ALTER TABLE learning_tasks
    ADD COLUMN status_managed_by_study_session BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN status_before_study_session_completion VARCHAR(20);

ALTER TABLE learning_tasks
    ADD CONSTRAINT chk_learning_tasks_session_status_source
        CHECK (
            (status_managed_by_study_session = FALSE
                AND status_before_study_session_completion IS NULL)
            OR
            (status_managed_by_study_session = TRUE
                AND status = 'COMPLETED'
                AND status_before_study_session_completion IN ('PLANNED', 'IN_PROGRESS'))
        );
