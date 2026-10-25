ALTER TABLE topics
    ADD COLUMN owner_user_id BIGINT;

ALTER TABLE topics
    ADD CONSTRAINT fk_topics_owner
        FOREIGN KEY (owner_user_id) REFERENCES users (id);

CREATE INDEX idx_topics_owner
    ON topics (owner_user_id);

COMMENT ON COLUMN topics.owner_user_id IS
    'Nullable only for legacy topics awaiting explicit owner assignment';
