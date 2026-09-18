-- Migration V15: Create coin_rewards, coin_reward_sizes, and reward_redemptions tables

CREATE TABLE IF NOT EXISTS coin_rewards (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    description TEXT NOT NULL,
    redeem_coins INTEGER NOT NULL,
    stock INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS coin_reward_sizes (
    id UUID PRIMARY KEY,
    coin_reward_id UUID NOT NULL REFERENCES coin_rewards(id) ON DELETE CASCADE,
    size VARCHAR(20) NOT NULL,
    stock INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_coin_reward_size UNIQUE (coin_reward_id, size)
);

CREATE INDEX IF NOT EXISTS ix_coin_reward_sizes_reward 
    ON coin_reward_sizes (coin_reward_id);

CREATE TABLE IF NOT EXISTS reward_redemptions (
    id UUID PRIMARY KEY,
    reference_number VARCHAR(60) NOT NULL UNIQUE,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE RESTRICT,
    coin_reward_id UUID NOT NULL REFERENCES coin_rewards(id) ON DELETE RESTRICT,
    coin_reward_size_id UUID REFERENCES coin_reward_sizes(id) ON DELETE RESTRICT,
    item_name VARCHAR(100) NOT NULL,
    selected_size VARCHAR(20),
    coins_used INTEGER NOT NULL,
    phone_number VARCHAR(30) NOT NULL,
    delivery_address TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'CONFIRMED',
    estimated_delivery_from DATE NOT NULL,
    estimated_delivery_to DATE NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS ix_reward_redemptions_user 
    ON reward_redemptions (user_id, created_at DESC);
