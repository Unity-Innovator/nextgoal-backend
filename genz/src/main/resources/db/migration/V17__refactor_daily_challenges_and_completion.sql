-- Migration V17: Refactor daily challenges, rules, and user completion tracking

-- 1. Add rule columns to daily_challenges
ALTER TABLE daily_challenges
    ADD COLUMN IF NOT EXISTS rule_type VARCHAR(50),
    ADD COLUMN IF NOT EXISTS minimum_amount NUMERIC(19, 4),
    ADD COLUMN IF NOT EXISTS target_count INTEGER DEFAULT 1;

-- 2. Add xp_awarded to user_daily_challenges
ALTER TABLE user_daily_challenges
    ADD COLUMN IF NOT EXISTS xp_awarded INTEGER DEFAULT 0;

-- 3. Replace single-challenge constraint with (user_id, challenge_id, challenge_date)
ALTER TABLE user_daily_challenges
    DROP CONSTRAINT IF EXISTS uk_user_daily_challenge;

ALTER TABLE user_daily_challenges
    DROP CONSTRAINT IF EXISTS uk_user_challenge_date;

ALTER TABLE user_daily_challenges
    ADD CONSTRAINT uk_user_challenge_date UNIQUE (user_id, challenge_id, challenge_date);

CREATE INDEX IF NOT EXISTS ix_user_daily_challenges_user_status_date
    ON user_daily_challenges (user_id, status, challenge_date);
