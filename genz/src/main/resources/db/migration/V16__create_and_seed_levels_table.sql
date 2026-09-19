-- Migration V16: Create and seed levels table for User Level System

CREATE TABLE IF NOT EXISTS levels (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    level_number INTEGER NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    required_xp INTEGER NOT NULL UNIQUE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS ix_levels_level_number 
    ON levels (level_number);

CREATE INDEX IF NOT EXISTS ix_levels_required_xp 
    ON levels (required_xp ASC);

-- Seed the initial 10 levels
INSERT INTO levels (id, level_number, name, required_xp) VALUES
    ('00000000-0000-0000-0000-000000000001', 1, 'Rookie Saver', 0),
    ('00000000-0000-0000-0000-000000000002', 2, 'Smart Saver', 100),
    ('00000000-0000-0000-0000-000000000003', 3, 'Goal Builder', 300),
    ('00000000-0000-0000-0000-000000000004', 4, 'Saving Master', 600),
    ('00000000-0000-0000-0000-000000000005', 5, 'Financial Explorer', 1000),
    ('00000000-0000-0000-0000-000000000006', 6, 'Wealth Builder', 1500),
    ('00000000-0000-0000-0000-000000000007', 7, 'Capital Champion', 2200),
    ('00000000-0000-0000-0000-000000000008', 8, 'Investment Prodigy', 3000),
    ('00000000-0000-0000-0000-000000000009', 9, 'Diamond Saver', 4000),
    ('00000000-0000-0000-0000-000000000010', 10, 'Legendary Saver', 5000);
