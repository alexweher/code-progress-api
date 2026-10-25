ALTER TABLE learning_tasks
    ADD COLUMN deadline DATE,
    ADD COLUMN priority VARCHAR(10) NOT NULL DEFAULT 'MEDIUM';

ALTER TABLE learning_tasks
    ADD CONSTRAINT chk_learning_tasks_priority
        CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH'));

CREATE INDEX idx_learning_tasks_topic_deadline
    ON learning_tasks (topic_id, deadline)
    INCLUDE (status, priority)
    WHERE deadline IS NOT NULL;
