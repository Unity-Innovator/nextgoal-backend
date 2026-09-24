-- Drop restrictive Hibernate-generated check constraints on Enum columns
-- to allow new Java enum values without DB insert errors.

ALTER TABLE IF EXISTS notifications DROP CONSTRAINT IF EXISTS notifications_type_check;
ALTER TABLE IF EXISTS goal_invitations DROP CONSTRAINT IF EXISTS goal_invitations_status_check;
ALTER TABLE IF EXISTS goals DROP CONSTRAINT IF EXISTS goals_goal_type_check;
ALTER TABLE IF EXISTS saving_plans DROP CONSTRAINT IF EXISTS saving_plans_frequency_check;
ALTER TABLE IF EXISTS saving_plans DROP CONSTRAINT IF EXISTS saving_plans_status_check;
ALTER TABLE IF EXISTS badges DROP CONSTRAINT IF EXISTS badges_category_check;
ALTER TABLE IF EXISTS badges DROP CONSTRAINT IF EXISTS badges_rarity_check;
ALTER TABLE IF EXISTS badges DROP CONSTRAINT IF EXISTS badges_requirement_type_check;
ALTER TABLE IF EXISTS xp_transactions DROP CONSTRAINT IF EXISTS xp_transactions_activity_type_check;
ALTER TABLE IF EXISTS user_daily_challenges DROP CONSTRAINT IF EXISTS user_daily_challenges_status_check;
ALTER TABLE IF EXISTS user_daily_challenges DROP CONSTRAINT IF EXISTS uk_user_challenge_date;
ALTER TABLE IF EXISTS user_daily_challenges ADD CONSTRAINT uk_user_challenge_date UNIQUE (user_id, challenge_id, challenge_date);
ALTER TABLE IF EXISTS users DROP CONSTRAINT IF EXISTS users_role_check;
