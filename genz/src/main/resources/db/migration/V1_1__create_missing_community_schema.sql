-- Bridge legacy databases that were baselined at Flyway version 1 before all
-- community and chat entities had been materialized by Hibernate.  Keep this
-- migration before V2, which adds the production constraints and indexes.

CREATE TABLE IF NOT EXISTS friend_requests (
    request_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    receiver_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'CANCELLED')),
    responded_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS friendships (
    friendship_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_one_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    user_two_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    request_id UUID UNIQUE REFERENCES friend_requests(request_id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS user_blocks (
    block_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    blocker_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    blocked_user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS post_media (
    media_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id UUID NOT NULL REFERENCES posts(post_id) ON DELETE CASCADE,
    media_type VARCHAR(20) NOT NULL CHECK (media_type IN ('IMAGE', 'VIDEO')),
    media_url TEXT NOT NULL,
    thumbnail_url TEXT,
    width_px INTEGER CHECK (width_px IS NULL OR width_px > 0),
    height_px INTEGER CHECK (height_px IS NULL OR height_px > 0),
    duration_ms INTEGER CHECK (duration_ms IS NULL OR duration_ms >= 0),
    sort_order SMALLINT NOT NULL DEFAULT 0 CHECK (sort_order >= 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_post_media_post
    ON post_media (post_id, sort_order);

CREATE TABLE IF NOT EXISTS comment_likes (
    comment_like_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    comment_id UUID NOT NULL REFERENCES post_comments(comment_id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS community_notifications (
    notification_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    actor_id UUID REFERENCES users(user_id) ON DELETE SET NULL,
    notification_type VARCHAR(30) NOT NULL
        CHECK (notification_type IN (
            'FRIEND_REQUEST', 'FRIEND_ACCEPTED', 'POST_LIKED',
            'POST_COMMENTED', 'COMMENT_REPLIED', 'POST_SHARED', 'CHAT_MESSAGE'
        )),
    reference_type VARCHAR(30),
    reference_id UUID,
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    read_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS chat_conversations (
    conversation_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_type VARCHAR(10) NOT NULL
        CHECK (conversation_type IN ('DIRECT', 'GROUP')),
    created_by UUID NOT NULL REFERENCES users(user_id) ON DELETE RESTRICT,
    title VARCHAR(120),
    image_url TEXT,
    direct_key VARCHAR(73),
    last_message_id UUID,
    last_message_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CHECK (
        (conversation_type = 'DIRECT' AND direct_key IS NOT NULL AND title IS NULL)
        OR (conversation_type = 'GROUP' AND direct_key IS NULL)
    )
);

CREATE TABLE IF NOT EXISTS chat_conversation_members (
    conversation_id UUID NOT NULL REFERENCES chat_conversations(conversation_id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    member_role VARCHAR(10) NOT NULL DEFAULT 'MEMBER'
        CHECK (member_role IN ('OWNER', 'ADMIN', 'MEMBER')),
    membership_status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE'
        CHECK (membership_status IN ('ACTIVE', 'LEFT', 'REMOVED')),
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    left_at TIMESTAMP WITH TIME ZONE,
    last_read_message_id UUID,
    last_read_at TIMESTAMP WITH TIME ZONE,
    muted_until TIMESTAMP WITH TIME ZONE,
    is_archived BOOLEAN NOT NULL DEFAULT false,
    PRIMARY KEY (conversation_id, user_id)
);

CREATE TABLE IF NOT EXISTS chat_messages (
    message_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES chat_conversations(conversation_id) ON DELETE CASCADE,
    sender_id UUID,
    reply_to_message_id UUID,
    client_message_id UUID,
    message_type VARCHAR(15) NOT NULL DEFAULT 'TEXT'
        CHECK (message_type IN ('TEXT', 'IMAGE', 'VIDEO', 'FILE', 'VOICE', 'SYSTEM')),
    body TEXT,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    status VARCHAR(10) NOT NULL DEFAULT 'SENT'
        CHECK (status IN ('SENT', 'EDITED', 'DELETED')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    edited_at TIMESTAMP WITH TIME ZONE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT ux_chat_messages_conversation_message
        UNIQUE (conversation_id, message_id),
    CONSTRAINT fk_chat_message_sender_membership
        FOREIGN KEY (conversation_id, sender_id)
        REFERENCES chat_conversation_members(conversation_id, user_id),
    CONSTRAINT fk_chat_message_reply_same_conversation
        FOREIGN KEY (conversation_id, reply_to_message_id)
        REFERENCES chat_messages(conversation_id, message_id),
    CHECK (
        (message_type = 'SYSTEM' AND sender_id IS NULL)
        OR (message_type <> 'SYSTEM' AND sender_id IS NOT NULL)
    ),
    CHECK (body IS NOT NULL OR message_type IN ('IMAGE', 'VIDEO', 'FILE', 'VOICE'))
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_chat_conversations_last_message'
          AND conrelid = 'chat_conversations'::regclass
    ) THEN
        ALTER TABLE chat_conversations
            ADD CONSTRAINT fk_chat_conversations_last_message
            FOREIGN KEY (conversation_id, last_message_id)
            REFERENCES chat_messages(conversation_id, message_id)
            DEFERRABLE INITIALLY DEFERRED;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_chat_members_last_read_message'
          AND conrelid = 'chat_conversation_members'::regclass
    ) THEN
        ALTER TABLE chat_conversation_members
            ADD CONSTRAINT fk_chat_members_last_read_message
            FOREIGN KEY (conversation_id, last_read_message_id)
            REFERENCES chat_messages(conversation_id, message_id)
            DEFERRABLE INITIALLY DEFERRED;
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS chat_message_attachments (
    attachment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id UUID NOT NULL REFERENCES chat_messages(message_id) ON DELETE CASCADE,
    attachment_type VARCHAR(10) NOT NULL
        CHECK (attachment_type IN ('IMAGE', 'VIDEO', 'FILE', 'VOICE')),
    file_url TEXT NOT NULL,
    thumbnail_url TEXT,
    original_name VARCHAR(255),
    mime_type VARCHAR(100),
    file_size_bytes BIGINT CHECK (file_size_bytes IS NULL OR file_size_bytes >= 0),
    width_px INTEGER CHECK (width_px IS NULL OR width_px > 0),
    height_px INTEGER CHECK (height_px IS NULL OR height_px > 0),
    duration_ms INTEGER CHECK (duration_ms IS NULL OR duration_ms >= 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_chat_attachments_message
    ON chat_message_attachments (message_id);

CREATE TABLE IF NOT EXISTS chat_message_reactions (
    reaction_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id UUID NOT NULL REFERENCES chat_messages(message_id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    emoji VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_chat_reactions_message
    ON chat_message_reactions (message_id);

CREATE TABLE IF NOT EXISTS chat_message_receipts (
    conversation_id UUID NOT NULL,
    message_id UUID NOT NULL REFERENCES chat_messages(message_id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    delivered_at TIMESTAMP WITH TIME ZONE,
    read_at TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (message_id, user_id),
    CONSTRAINT fk_chat_receipt_message_conversation
        FOREIGN KEY (conversation_id, message_id)
        REFERENCES chat_messages(conversation_id, message_id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_receipt_recipient_membership
        FOREIGN KEY (conversation_id, user_id)
        REFERENCES chat_conversation_members(conversation_id, user_id) ON DELETE CASCADE,
    CHECK (read_at IS NULL OR delivered_at IS NOT NULL)
);
