-- Executable baseline for a brand-new database.
--
-- Existing deployments already recorded V1 and ignore this B1 migration.
-- Empty databases execute B1 instead of the marker-only V1, then continue with
-- V1.1 and later migrations. Keep this file at version 1 so it represents the
-- legacy Hibernate-managed schema that the versioned migrations build upon.

CREATE TABLE users (
    user_id UUID PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(255) NOT NULL UNIQUE,
    date_of_birth DATE NOT NULL,
    gender VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255),
    pin_hash VARCHAR(255),
    status VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    energy INTEGER NOT NULL DEFAULT 100,
    total_xp INTEGER NOT NULL DEFAULT 0,
    energy_reset_date DATE,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE TABLE otps (
    otp_id UUID PRIMARY KEY,
    phone_number VARCHAR(255) NOT NULL,
    otp_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    attempts INTEGER NOT NULL,
    used BOOLEAN NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

CREATE INDEX ix_otps_phone_created ON otps (phone_number, created_at DESC);

CREATE TABLE password_reset_tokens (
    token_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

CREATE INDEX ix_password_reset_tokens_hash ON password_reset_tokens (token_hash);
CREATE INDEX ix_password_reset_tokens_user ON password_reset_tokens (user_id);

CREATE TABLE goals (
    goal_id UUID PRIMARY KEY,
    goal_name VARCHAR(100) NOT NULL,
    target_amount NUMERIC(19, 4) NOT NULL,
    current_amount NUMERIC(19, 4) NOT NULL DEFAULT 0,
    target_date DATE NOT NULL,
    category VARCHAR(255) NOT NULL,
    image_url VARCHAR(500),
    remain_balance NUMERIC(38, 2),
    status VARCHAR(255) NOT NULL,
    goal_type VARCHAR(50) NOT NULL DEFAULT 'FLEXIBLE',
    owner_id UUID NOT NULL REFERENCES users(user_id),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    completed_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX ix_goals_owner ON goals (owner_id);

CREATE TABLE goal_members (
    goal_member_id UUID PRIMARY KEY,
    goal_id UUID NOT NULL REFERENCES goals(goal_id),
    user_id UUID NOT NULL REFERENCES users(user_id),
    role VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    joined_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX ix_goal_members_goal ON goal_members (goal_id);
CREATE INDEX ix_goal_members_user ON goal_members (user_id);

CREATE TABLE goal_invitations (
    invitation_id UUID PRIMARY KEY,
    goal_id UUID NOT NULL REFERENCES goals(goal_id),
    inviter_id UUID NOT NULL REFERENCES users(user_id),
    invitee_id UUID NOT NULL REFERENCES users(user_id),
    status VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    responded_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX ix_goal_invitations_invitee ON goal_invitations (invitee_id, status);

CREATE TABLE group_wallets (
    wallet_id UUID PRIMARY KEY,
    balance NUMERIC(19, 2) NOT NULL,
    creator_id UUID NOT NULL REFERENCES users(user_id),
    goal_id UUID NOT NULL UNIQUE REFERENCES goals(goal_id),
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    currency VARCHAR(255) NOT NULL
);

CREATE TABLE withdrawal_requests (
    request_id UUID PRIMARY KEY,
    goal_id UUID NOT NULL REFERENCES goals(goal_id),
    wallet_id UUID NOT NULL REFERENCES group_wallets(wallet_id),
    requested_by UUID NOT NULL REFERENCES users(user_id),
    amount NUMERIC(19, 4) NOT NULL,
    reason VARCHAR(500),
    status VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    resolved_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE TABLE withdrawal_approvals (
    approval_id UUID PRIMARY KEY,
    request_id UUID NOT NULL REFERENCES withdrawal_requests(request_id),
    user_id UUID NOT NULL REFERENCES users(user_id),
    decision VARCHAR(255) NOT NULL,
    decided_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

CREATE TABLE wallet_transactions (
    transaction_id UUID PRIMARY KEY,
    wallet_id UUID NOT NULL REFERENCES group_wallets(wallet_id),
    user_id UUID NOT NULL REFERENCES users(user_id),
    amount NUMERIC(19, 2) NOT NULL,
    transaction_type VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    description VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

CREATE TABLE payment_methods (
    payment_method_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(user_id),
    method_type VARCHAR(255) NOT NULL,
    provider VARCHAR(100) NOT NULL,
    account_name VARCHAR(200) NOT NULL,
    account_number_masked VARCHAR(50) NOT NULL,
    account_token VARCHAR(500),
    currency VARCHAR(255) NOT NULL,
    is_default BOOLEAN,
    status VARCHAR(255) NOT NULL,
    verified_at TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE TABLE goal_contributions (
    contribution_id UUID PRIMARY KEY,
    goal_id UUID NOT NULL REFERENCES goals(goal_id),
    goal_member_id UUID NOT NULL REFERENCES goal_members(goal_member_id),
    amount NUMERIC(19, 4) NOT NULL,
    note VARCHAR(255),
    contributed_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

CREATE TABLE group_wallet_transactions (
    group_wallet_transaction_id UUID PRIMARY KEY,
    group_wallet_id UUID NOT NULL REFERENCES group_wallets(wallet_id),
    goal_member_id UUID NOT NULL REFERENCES goal_members(goal_member_id),
    withdrawal_request_id UUID REFERENCES withdrawal_requests(request_id),
    transaction_type VARCHAR(255) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    currency VARCHAR(255) NOT NULL,
    balance_before NUMERIC(19, 4) NOT NULL,
    balance_after NUMERIC(19, 4) NOT NULL,
    status VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

CREATE TABLE user_settings (
    settings_id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES users(user_id),
    language VARCHAR(255) NOT NULL,
    theme VARCHAR(255) NOT NULL,
    bill_reminder BOOLEAN NOT NULL,
    avatar_url VARCHAR(255),
    goal_alert BOOLEAN NOT NULL,
    lesson_reminder BOOLEAN NOT NULL,
    ai_tips BOOLEAN NOT NULL,
    daily_challenge_enabled BOOLEAN NOT NULL DEFAULT true,
    face_id_enabled BOOLEAN NOT NULL,
    auto_lock_minutes INTEGER NOT NULL,
    push_notifications_enabled BOOLEAN NOT NULL,
    sms_notifications_enabled BOOLEAN NOT NULL,
    email_notifications_enabled BOOLEAN NOT NULL,
    profile_visibility VARCHAR(255) NOT NULL,
    show_savings_goals_publicly BOOLEAN NOT NULL,
    show_full_name_on_posts BOOLEAN NOT NULL,
    allow_comments_on_posts BOOLEAN NOT NULL,
    preferred_currency VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE TABLE daily_challenges (
    challenge_id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    challenge_type VARCHAR(255) NOT NULL,
    xp_reward INTEGER,
    is_active BOOLEAN NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

CREATE TABLE user_daily_challenges (
    user_daily_challenge_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(user_id),
    challenge_id UUID NOT NULL REFERENCES daily_challenges(challenge_id),
    status VARCHAR(255) NOT NULL,
    completed_at TIMESTAMP WITHOUT TIME ZONE,
    challenge_date DATE NOT NULL,
    CONSTRAINT uk_user_daily_challenge UNIQUE (user_id, challenge_date)
);

CREATE TABLE badges (
    badge_id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    icon_url VARCHAR(255),
    category VARCHAR(50) NOT NULL DEFAULT 'GETTING_STARTED',
    requirement_type VARCHAR(255) NOT NULL,
    requirement_value INTEGER,
    xp_reward INTEGER NOT NULL DEFAULT 0,
    rarity VARCHAR(50) NOT NULL DEFAULT 'COMMON',
    active BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE user_badges (
    user_badge_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(user_id),
    badge_id UUID NOT NULL REFERENCES badges(badge_id),
    earned_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT uk_user_badge UNIQUE (user_id, badge_id)
);

CREATE TABLE saving_milestones (
    id UUID PRIMARY KEY,
    goal_id UUID NOT NULL REFERENCES goals(goal_id),
    percentage NUMERIC(38, 2) NOT NULL,
    amount_required NUMERIC(19, 4) NOT NULL,
    status VARCHAR(255) NOT NULL,
    reached_at TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE TABLE xp_transactions (
    xp_transaction_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(user_id),
    activity_type VARCHAR(255) NOT NULL,
    reference_id UUID,
    points INTEGER NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

CREATE TABLE user_rewards (
    reward_id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES users(user_id),
    total_xp INTEGER NOT NULL DEFAULT 0,
    level INTEGER NOT NULL DEFAULT 1,
    current_streak INTEGER NOT NULL DEFAULT 0,
    longest_streak INTEGER NOT NULL DEFAULT 0,
    last_activity_date DATE,
    updated_at TIMESTAMP WITHOUT TIME ZONE
);

-- V1.1 builds the newer community tables on top of these four base tables.
CREATE TABLE posts (
    post_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(user_id),
    goal_id UUID REFERENCES goals(goal_id),
    shared_post_id UUID REFERENCES posts(post_id),
    post_type VARCHAR(30) NOT NULL DEFAULT 'LIFE_UPDATE',
    content TEXT,
    visibility VARCHAR(20) NOT NULL DEFAULT 'PUBLIC',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    is_pinned BOOLEAN NOT NULL DEFAULT false,
    like_count INTEGER NOT NULL DEFAULT 0,
    comment_count INTEGER NOT NULL DEFAULT 0,
    share_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE post_comments (
    comment_id UUID PRIMARY KEY,
    post_id UUID NOT NULL REFERENCES posts(post_id),
    user_id UUID NOT NULL REFERENCES users(user_id),
    parent_comment_id UUID REFERENCES post_comments(comment_id),
    content TEXT NOT NULL DEFAULT '',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    like_count INTEGER NOT NULL DEFAULT 0,
    reply_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE post_likes (
    like_id UUID PRIMARY KEY,
    post_id UUID NOT NULL REFERENCES posts(post_id),
    user_id UUID NOT NULL REFERENCES users(user_id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT ux_post_likes_user UNIQUE (post_id, user_id)
);

CREATE TABLE post_reports (
    report_id UUID PRIMARY KEY,
    post_id UUID NOT NULL REFERENCES posts(post_id),
    reported_by UUID NOT NULL REFERENCES users(user_id),
    reason VARCHAR(30) NOT NULL,
    details TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewed_by UUID REFERENCES users(user_id),
    reviewed_at TIMESTAMP WITH TIME ZONE,
    moderation_note VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT ux_post_reports_reporter UNIQUE (post_id, reported_by)
);
