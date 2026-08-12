-- Drop restrictive Hibernate-generated check constraints on Enum columns
-- to allow new Java enum values without DB insert errors.

ALTER TABLE IF EXISTS notifications DROP CONSTRAINT IF EXISTS notifications_type_check;
ALTER TABLE IF EXISTS goal_invitations DROP CONSTRAINT IF EXISTS goal_invitations_status_check;
ALTER TABLE IF EXISTS goals DROP CONSTRAINT IF EXISTS goals_goal_type_check;
ALTER TABLE IF EXISTS saving_plans DROP CONSTRAINT IF EXISTS saving_plans_frequency_check;
ALTER TABLE IF EXISTS saving_plans DROP CONSTRAINT IF EXISTS saving_plans_status_check;
