DO $$
DECLARE
    ownerless_topic_count BIGINT;
BEGIN
    SELECT COUNT(*)
    INTO ownerless_topic_count
    FROM topics
    WHERE owner_user_id IS NULL;

    IF ownerless_topic_count > 0 THEN
        RAISE EXCEPTION
            'Cannot require topics.owner_user_id: % topic(s) have no owner',
            ownerless_topic_count;
    END IF;
END
$$;

ALTER TABLE topics
    ALTER COLUMN owner_user_id SET NOT NULL;

COMMENT ON COLUMN topics.owner_user_id IS
    'Required owner of the topic';
