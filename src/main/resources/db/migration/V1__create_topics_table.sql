CREATE TABLE topics (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(120) NOT NULL,
    description VARCHAR(2000),
    status VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_topics_title_not_blank CHECK (btrim(title) <> ''),
    CONSTRAINT chk_topics_status CHECK (status IN ('PLANNED', 'IN_PROGRESS', 'COMPLETED'))
);
