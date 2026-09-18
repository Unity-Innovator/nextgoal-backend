-- Migration V14: Create Goal Milestone Rewards table and add coin_balance to users

ALTER TABLE users 
    ADD COLUMN IF NOT EXISTS coin_balance INTEGER NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS goal_milestone_rewards (
    id UUID PRIMARY KEY,
    goal_id UUID NOT NULL REFERENCES goals(goal_id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    milestone VARCHAR(50) NOT NULL,
    coins_earned INTEGER NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

-- Intermediate milestones (25%, 50%, 75%) can only be awarded once per goal:
CREATE UNIQUE INDEX IF NOT EXISTS uk_goal_milestone_intermediate 
    ON goal_milestone_rewards (goal_id, milestone) 
    WHERE milestone IN ('MILESTONE_25', 'MILESTONE_50', 'MILESTONE_75');

-- Completion milestone (100%) can only be awarded once per user per goal:
CREATE UNIQUE INDEX IF NOT EXISTS uk_goal_milestone_completion 
    ON goal_milestone_rewards (goal_id, milestone, user_id) 
    WHERE milestone = 'MILESTONE_100';

CREATE INDEX IF NOT EXISTS ix_goal_milestone_rewards_goal 
    ON goal_milestone_rewards (goal_id);

CREATE INDEX IF NOT EXISTS ix_goal_milestone_rewards_user 
    ON goal_milestone_rewards (user_id);
