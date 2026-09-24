-- Community production hardening for PostgreSQL.
-- This migration intentionally fails when duplicate or invalid legacy data
-- violates a new invariant. Repair that data before retrying the deployment.

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS role VARCHAR(20) NOT NULL DEFAULT 'USER';

-- Hibernate used to update these tables incrementally, so legacy databases can
-- contain only part of the current community schema. Production uses
-- ddl-auto=validate. Materialize every non-key column used by the current post
-- entities and by the constraints/indexes below before Hibernate validates it.
ALTER TABLE posts
    ADD COLUMN IF NOT EXISTS goal_id UUID REFERENCES goals(goal_id);
ALTER TABLE posts
    ADD COLUMN IF NOT EXISTS shared_post_id UUID REFERENCES posts(post_id);
ALTER TABLE posts
    ADD COLUMN IF NOT EXISTS post_type VARCHAR(30) NOT NULL DEFAULT 'LIFE_UPDATE';
ALTER TABLE posts
    ADD COLUMN IF NOT EXISTS content TEXT;
ALTER TABLE posts
    ADD COLUMN IF NOT EXISTS visibility VARCHAR(20) NOT NULL DEFAULT 'PUBLIC';
ALTER TABLE posts
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE posts
    ADD COLUMN IF NOT EXISTS is_pinned BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE posts
    ADD COLUMN IF NOT EXISTS like_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE posts
    ADD COLUMN IF NOT EXISTS comment_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE posts
    ADD COLUMN IF NOT EXISTS share_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE posts
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now();
ALTER TABLE posts
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now();
ALTER TABLE posts
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE post_comments
    ADD COLUMN IF NOT EXISTS parent_comment_id UUID REFERENCES post_comments(comment_id);
ALTER TABLE post_comments
    ADD COLUMN IF NOT EXISTS content TEXT NOT NULL DEFAULT '';
ALTER TABLE post_comments
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE post_comments
    ADD COLUMN IF NOT EXISTS like_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE post_comments
    ADD COLUMN IF NOT EXISTS reply_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE post_comments
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now();
ALTER TABLE post_comments
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now();
ALTER TABLE post_comments
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE post_likes
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now();

ALTER TABLE post_reports
    ADD COLUMN IF NOT EXISTS details TEXT;
ALTER TABLE post_reports
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'PENDING';
ALTER TABLE post_reports
    ADD COLUMN IF NOT EXISTS reviewed_by UUID REFERENCES users(user_id) ON DELETE SET NULL;
ALTER TABLE post_reports
    ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE post_reports
    ADD COLUMN IF NOT EXISTS moderation_note VARCHAR(1000);
ALTER TABLE post_reports
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now();

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'ck_users_role' AND conrelid = 'users'::regclass
    ) THEN
        ALTER TABLE users ADD CONSTRAINT ck_users_role CHECK (role IN ('USER', 'ADMIN'));
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS comment_reports (
    report_id UUID PRIMARY KEY,
    comment_id UUID NOT NULL REFERENCES post_comments(comment_id) ON DELETE CASCADE,
    reported_by UUID NOT NULL REFERENCES users(user_id) ON DELETE RESTRICT,
    reason VARCHAR(30) NOT NULL,
    details TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewed_by UUID REFERENCES users(user_id) ON DELETE SET NULL,
    reviewed_at TIMESTAMP WITH TIME ZONE,
    moderation_note VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'ck_friend_requests_distinct_users'
          AND conrelid = 'friend_requests'::regclass
    ) THEN
        ALTER TABLE friend_requests
            ADD CONSTRAINT ck_friend_requests_distinct_users CHECK (sender_id <> receiver_id);
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'ck_friendships_ordered_users'
          AND conrelid = 'friendships'::regclass
    ) THEN
        ALTER TABLE friendships
            ADD CONSTRAINT ck_friendships_ordered_users CHECK (user_one_id < user_two_id);
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'ck_user_blocks_distinct_users'
          AND conrelid = 'user_blocks'::regclass
    ) THEN
        ALTER TABLE user_blocks
            ADD CONSTRAINT ck_user_blocks_distinct_users CHECK (blocker_id <> blocked_user_id);
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'ck_posts_nonnegative_counters'
          AND conrelid = 'posts'::regclass
    ) THEN
        ALTER TABLE posts ADD CONSTRAINT ck_posts_nonnegative_counters
            CHECK (like_count >= 0 AND comment_count >= 0 AND share_count >= 0);
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'ck_posts_not_self_shared'
          AND conrelid = 'posts'::regclass
    ) THEN
        ALTER TABLE posts ADD CONSTRAINT ck_posts_not_self_shared
            CHECK (shared_post_id IS NULL OR shared_post_id <> post_id);
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'ck_post_comments_nonnegative_counters'
          AND conrelid = 'post_comments'::regclass
    ) THEN
        ALTER TABLE post_comments ADD CONSTRAINT ck_post_comments_nonnegative_counters
            CHECK (like_count >= 0 AND reply_count >= 0);
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'ck_community_notifications_reference_pair'
          AND conrelid = 'community_notifications'::regclass
    ) THEN
        ALTER TABLE community_notifications ADD CONSTRAINT ck_community_notifications_reference_pair
            CHECK ((reference_type IS NULL) = (reference_id IS NULL));
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'ck_comment_reports_reason'
          AND conrelid = 'comment_reports'::regclass
    ) THEN
        ALTER TABLE comment_reports ADD CONSTRAINT ck_comment_reports_reason
            CHECK (reason IN ('SPAM', 'HARASSMENT', 'INAPPROPRIATE', 'MISINFORMATION', 'OTHER'));
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'ck_comment_reports_status'
          AND conrelid = 'comment_reports'::regclass
    ) THEN
        ALTER TABLE comment_reports ADD CONSTRAINT ck_comment_reports_status
            CHECK (status IN ('PENDING', 'REVIEWED', 'DISMISSED', 'ACTION_TAKEN'));
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS ux_friend_requests_pending_pair
    ON friend_requests (LEAST(sender_id, receiver_id), GREATEST(sender_id, receiver_id))
    WHERE status = 'PENDING';
CREATE UNIQUE INDEX IF NOT EXISTS ux_friendships_pair
    ON friendships (user_one_id, user_two_id);
CREATE UNIQUE INDEX IF NOT EXISTS ux_user_blocks_pair
    ON user_blocks (blocker_id, blocked_user_id);
CREATE UNIQUE INDEX IF NOT EXISTS ux_post_media_order
    ON post_media (post_id, sort_order);
CREATE UNIQUE INDEX IF NOT EXISTS ux_post_likes_user
    ON post_likes (post_id, user_id);
CREATE UNIQUE INDEX IF NOT EXISTS ux_comment_likes_user
    ON comment_likes (comment_id, user_id);
CREATE UNIQUE INDEX IF NOT EXISTS ux_post_reports_reporter
    ON post_reports (post_id, reported_by);
CREATE UNIQUE INDEX IF NOT EXISTS ux_comment_reports_reporter
    ON comment_reports (comment_id, reported_by);
CREATE UNIQUE INDEX IF NOT EXISTS ux_chat_conversations_direct_key
    ON chat_conversations (direct_key) WHERE direct_key IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS ux_chat_messages_client_id
    ON chat_messages (sender_id, client_message_id) WHERE client_message_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS ux_chat_reaction_user_emoji
    ON chat_message_reactions (message_id, user_id, emoji);

CREATE INDEX IF NOT EXISTS ix_friend_requests_receiver_status
    ON friend_requests (receiver_id, status, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_friend_requests_sender_status
    ON friend_requests (sender_id, status, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_friendships_user_one
    ON friendships (user_one_id, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_friendships_user_two
    ON friendships (user_two_id, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_user_blocks_blocked_user
    ON user_blocks (blocked_user_id);

CREATE INDEX IF NOT EXISTS ix_posts_author_created
    ON posts (user_id, created_at DESC, post_id DESC);
CREATE INDEX IF NOT EXISTS ix_posts_public_feed
    ON posts (created_at DESC, post_id DESC)
    WHERE status = 'ACTIVE' AND visibility = 'PUBLIC';
CREATE INDEX IF NOT EXISTS ix_posts_friends_feed
    ON posts (user_id, created_at DESC, post_id DESC)
    WHERE status = 'ACTIVE' AND visibility IN ('PUBLIC', 'FRIENDS_ONLY');
CREATE INDEX IF NOT EXISTS ix_posts_shared_post
    ON posts (shared_post_id) WHERE shared_post_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS ix_post_likes_user
    ON post_likes (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_post_comments_page
    ON post_comments (post_id, created_at, comment_id)
    WHERE parent_comment_id IS NULL AND status = 'ACTIVE';
CREATE INDEX IF NOT EXISTS ix_post_comment_replies_page
    ON post_comments (parent_comment_id, created_at, comment_id)
    WHERE parent_comment_id IS NOT NULL AND status = 'ACTIVE';
CREATE INDEX IF NOT EXISTS ix_comment_likes_user
    ON comment_likes (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_post_reports_moderation
    ON post_reports (status, created_at) WHERE status = 'PENDING';
CREATE INDEX IF NOT EXISTS ix_comment_reports_moderation
    ON comment_reports (status, created_at) WHERE status = 'PENDING';

CREATE INDEX IF NOT EXISTS ix_community_notifications_unread
    ON community_notifications (recipient_id, created_at DESC, notification_id DESC)
    WHERE read_at IS NULL;
CREATE INDEX IF NOT EXISTS ix_community_notifications_recipient_history
    ON community_notifications (recipient_id, created_at DESC, notification_id DESC);

CREATE INDEX IF NOT EXISTS ix_chat_conversations_recent
    ON chat_conversations (last_message_at DESC NULLS LAST, conversation_id DESC);
CREATE INDEX IF NOT EXISTS ix_chat_members_user_inbox
    ON chat_conversation_members (user_id, is_archived, conversation_id)
    WHERE membership_status = 'ACTIVE';
CREATE INDEX IF NOT EXISTS ix_chat_messages_page
    ON chat_messages (conversation_id, created_at DESC, message_id DESC);
CREATE INDEX IF NOT EXISTS ix_chat_messages_sender
    ON chat_messages (sender_id, created_at DESC) WHERE sender_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS ix_chat_receipts_user_unread
    ON chat_message_receipts (user_id, message_id) WHERE read_at IS NULL;
