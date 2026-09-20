-- Migration V18: Add target_value, reward_coins to daily_challenges and progress, reward_coins_awarded to user_daily_challenges

-- 1. Add target_value and reward_coins to daily_challenges
ALTER TABLE daily_challenges
    ADD COLUMN IF NOT EXISTS target_value NUMERIC(19, 4),
    ADD COLUMN IF NOT EXISTS reward_coins INTEGER DEFAULT 0;

-- 2. Add progress and reward_coins_awarded to user_daily_challenges
ALTER TABLE user_daily_challenges
    ADD COLUMN IF NOT EXISTS progress NUMERIC(19, 4) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS reward_coins_awarded INTEGER DEFAULT 0;
