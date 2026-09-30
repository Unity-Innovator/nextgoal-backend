-- Legacy Hibernate-generated checks predate SHARED_POST. Updating the Java
-- enum does not update those checks when production uses ddl-auto=validate.
ALTER TABLE posts
    DROP CONSTRAINT IF EXISTS posts_post_type_check;

ALTER TABLE posts
    ADD CONSTRAINT posts_post_type_check
    CHECK (post_type IN (
        'GOAL_SUCCESS',
        'MILESTONE',
        'TIP',
        'EXPERIENCE',
        'LIFE_UPDATE',
        'SHARED_POST'
    ));
