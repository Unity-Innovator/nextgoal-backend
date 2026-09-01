-- Complete the goal-domain schema introduced after the legacy production
-- database was last managed by Hibernate ddl-auto=update. V4 is already
-- deployed, so this repair intentionally lives in a new Flyway version.

-- Goal type was added after the original goals table. Backfill legacy goals so
-- application code never receives a null type.
ALTER TABLE goals
    ADD COLUMN IF NOT EXISTS goal_type VARCHAR(50) NOT NULL DEFAULT 'FLEXIBLE';
UPDATE goals SET goal_type = 'FLEXIBLE' WHERE goal_type IS NULL;
ALTER TABLE goals ALTER COLUMN goal_type SET DEFAULT 'FLEXIBLE';
ALTER TABLE goals ALTER COLUMN goal_type SET NOT NULL;

-- These tables were introduced by the same later goal/notification feature.
-- CREATE handles databases that never materialized them. The following ALTERs
-- also tolerate a table that a previous Hibernate startup created only partly.
CREATE TABLE IF NOT EXISTS saving_plans (
    plan_id UUID PRIMARY KEY,
    goal_id UUID NOT NULL UNIQUE REFERENCES goals(goal_id) ON DELETE CASCADE,
    contribution_amount NUMERIC(19, 4) NOT NULL,
    frequency VARCHAR(50) NOT NULL,
    next_contribution_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    contributed_this_period NUMERIC(19, 4) NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITHOUT TIME ZONE
);

ALTER TABLE saving_plans ADD COLUMN IF NOT EXISTS plan_id UUID;
ALTER TABLE saving_plans ADD COLUMN IF NOT EXISTS goal_id UUID;
ALTER TABLE saving_plans
    ADD COLUMN IF NOT EXISTS contribution_amount NUMERIC(19, 4) NOT NULL DEFAULT 0;
ALTER TABLE saving_plans
    ADD COLUMN IF NOT EXISTS frequency VARCHAR(50) NOT NULL DEFAULT 'MONTHLY';
ALTER TABLE saving_plans
    ADD COLUMN IF NOT EXISTS next_contribution_date DATE NOT NULL DEFAULT CURRENT_DATE;
ALTER TABLE saving_plans
    ADD COLUMN IF NOT EXISTS status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE saving_plans
    ADD COLUMN IF NOT EXISTS contributed_this_period NUMERIC(19, 4) NOT NULL DEFAULT 0;
ALTER TABLE saving_plans
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now();
ALTER TABLE saving_plans
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITHOUT TIME ZONE;

CREATE TABLE IF NOT EXISTS notifications (
    notification_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    message VARCHAR(500) NOT NULL,
    type VARCHAR(50) NOT NULL,
    reference_id UUID,
    is_read BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now()
);

ALTER TABLE notifications ADD COLUMN IF NOT EXISTS notification_id UUID;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS user_id UUID;
ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS title VARCHAR(255) NOT NULL DEFAULT '';
ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS message VARCHAR(500) NOT NULL DEFAULT '';
ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS type VARCHAR(50) NOT NULL DEFAULT 'GOAL_INVITATION';
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS reference_id UUID;
ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS is_read BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now();

CREATE INDEX IF NOT EXISTS ix_notifications_user_created
    ON notifications (user_id, created_at DESC);
