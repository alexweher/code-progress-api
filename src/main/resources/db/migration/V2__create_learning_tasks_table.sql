CREATE TABLE learning_tasks (
    id BIGSERIAL PRIMARY KEY,
    topic_id BIGINT NOT NULL,
    title VARCHAR(160) NOT NULL,
    description VARCHAR(3000),
    difficulty VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_learning_tasks_topic
        FOREIGN KEY (topic_id) REFERENCES topics (id) ON DELETE CASCADE,
    CONSTRAINT chk_learning_tasks_title_not_blank
        CHECK (btrim(title) <> ''),
    CONSTRAINT chk_learning_tasks_difficulty
        CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD')),
    CONSTRAINT chk_learning_tasks_status
        CHECK (status IN ('PLANNED', 'IN_PROGRESS', 'COMPLETED'))
);
