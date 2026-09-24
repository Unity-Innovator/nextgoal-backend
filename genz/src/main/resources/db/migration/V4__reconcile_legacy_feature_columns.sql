-- Reconcile columns introduced while the legacy production database was still
-- managed by Hibernate. Flyway V1 baselined that existing database, so these
-- changes need an explicit versioned migration now that production validates
-- the schema instead of mutating it.

-- Quiz energy and XP were added to the user model after the legacy schema was
-- first created.
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS energy INTEGER NOT NULL DEFAULT 100;
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS total_xp INTEGER NOT NULL DEFAULT 0;
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS energy_reset_date DATE;

-- Daily challenges can be toggled from user settings.
ALTER TABLE user_settings
    ADD COLUMN IF NOT EXISTS daily_challenge_enabled BOOLEAN NOT NULL DEFAULT true;

-- Achievement metadata was added together, but partially updated databases can
-- contain any subset of these columns. Add all four before Hibernate validates
-- the Badge entity so deployments do not fail one column at a time.
ALTER TABLE badges
    ADD COLUMN IF NOT EXISTS category VARCHAR(50) NOT NULL DEFAULT 'GETTING_STARTED';
ALTER TABLE badges
    ADD COLUMN IF NOT EXISTS xp_reward INTEGER NOT NULL DEFAULT 0;
ALTER TABLE badges
    ADD COLUMN IF NOT EXISTS rarity VARCHAR(50) NOT NULL DEFAULT 'COMMON';
ALTER TABLE badges
    ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT true;

-- A prior Hibernate update may have created a column without the current
-- required default/nullability. Backfill and normalize those partial states.
UPDATE users SET energy = 100 WHERE energy IS NULL;
UPDATE users SET total_xp = 0 WHERE total_xp IS NULL;
ALTER TABLE users ALTER COLUMN energy SET DEFAULT 100;
ALTER TABLE users ALTER COLUMN energy SET NOT NULL;
ALTER TABLE users ALTER COLUMN total_xp SET DEFAULT 0;
ALTER TABLE users ALTER COLUMN total_xp SET NOT NULL;

UPDATE user_settings
SET daily_challenge_enabled = true
WHERE daily_challenge_enabled IS NULL;
ALTER TABLE user_settings
    ALTER COLUMN daily_challenge_enabled SET DEFAULT true;
ALTER TABLE user_settings
    ALTER COLUMN daily_challenge_enabled SET NOT NULL;

UPDATE badges SET category = 'GETTING_STARTED' WHERE category IS NULL;
UPDATE badges SET xp_reward = 0 WHERE xp_reward IS NULL;
UPDATE badges SET rarity = 'COMMON' WHERE rarity IS NULL;
UPDATE badges SET active = true WHERE active IS NULL;
ALTER TABLE badges ALTER COLUMN category SET DEFAULT 'GETTING_STARTED';
ALTER TABLE badges ALTER COLUMN category SET NOT NULL;
ALTER TABLE badges ALTER COLUMN xp_reward SET DEFAULT 0;
ALTER TABLE badges ALTER COLUMN xp_reward SET NOT NULL;
ALTER TABLE badges ALTER COLUMN rarity SET DEFAULT 'COMMON';
ALTER TABLE badges ALTER COLUMN rarity SET NOT NULL;
ALTER TABLE badges ALTER COLUMN active SET DEFAULT true;
ALTER TABLE badges ALTER COLUMN active SET NOT NULL;
