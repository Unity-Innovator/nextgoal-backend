-- Add unique constraint to ensure a user cannot be added multiple times to the same goal
CREATE UNIQUE INDEX IF NOT EXISTS uk_goal_members_goal_user ON goal_members (goal_id, user_id);
