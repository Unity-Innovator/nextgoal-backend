-- Migration V23: Create Personal Wallets and reconcile wallet_transactions

-- 1. Create personal wallets table
CREATE TABLE IF NOT EXISTS wallets (
    wallet_id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES users(user_id) ON DELETE CASCADE,
    balance NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(20) NOT NULL DEFAULT 'USD',
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS ix_wallets_user_id ON wallets(user_id);

-- 2. Populate personal wallets for all existing users with 0.00 balance
-- (Safe migration: no unbacked money is invented for existing accounts)
INSERT INTO wallets (wallet_id, user_id, balance, currency, created_at, updated_at)
SELECT gen_random_uuid(), u.user_id, 0.00, 'USD', NOW(), NOW()
FROM users u
WHERE NOT EXISTS (SELECT 1 FROM wallets w WHERE w.user_id = u.user_id);

-- 3. Add goal_id, balance_before, balance_after, direction to wallet_transactions
ALTER TABLE wallet_transactions ADD COLUMN IF NOT EXISTS goal_id UUID REFERENCES goals(goal_id);
ALTER TABLE wallet_transactions ADD COLUMN IF NOT EXISTS balance_before NUMERIC(19, 2);
ALTER TABLE wallet_transactions ADD COLUMN IF NOT EXISTS balance_after NUMERIC(19, 2);
ALTER TABLE wallet_transactions ADD COLUMN IF NOT EXISTS direction VARCHAR(20);

-- Backfill goal_id for existing transactions from group_wallets if needed
UPDATE wallet_transactions wt
SET goal_id = gw.goal_id
FROM group_wallets gw
WHERE wt.wallet_id = gw.wallet_id AND wt.goal_id IS NULL;

-- Backfill direction and balance_before/after for legacy records
UPDATE wallet_transactions
SET balance_before = 0.00
WHERE balance_before IS NULL;

UPDATE wallet_transactions
SET balance_after = amount
WHERE balance_after IS NULL;

UPDATE wallet_transactions
SET direction = CASE
    WHEN transaction_type IN ('DEPOSIT', 'GOAL_DEPOSIT') THEN 'OUT'
    WHEN transaction_type IN ('WITHDRAWAL', 'GOAL_WITHDRAWAL', 'TOP_UP') THEN 'IN'
    ELSE 'OUT'
END
WHERE direction IS NULL;

-- Re-point wallet_id in wallet_transactions to the user's personal wallet
UPDATE wallet_transactions wt
SET wallet_id = w.wallet_id
FROM wallets w
WHERE wt.user_id = w.user_id;

-- Drop old foreign key constraint pointing to group_wallets if it exists
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN (
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'wallet_transactions'::regclass
          AND contype = 'f'
          AND confrelid = 'group_wallets'::regclass
    ) LOOP
        EXECUTE 'ALTER TABLE wallet_transactions DROP CONSTRAINT ' || quote_ident(r.conname);
    END LOOP;
END $$;

-- Add foreign key constraint pointing to wallets
ALTER TABLE wallet_transactions
    DROP CONSTRAINT IF EXISTS fk_wallet_transactions_wallet;
ALTER TABLE wallet_transactions
    ADD CONSTRAINT fk_wallet_transactions_wallet
    FOREIGN KEY (wallet_id) REFERENCES wallets(wallet_id);

CREATE INDEX IF NOT EXISTS ix_wallet_transactions_goal_id ON wallet_transactions(goal_id);
CREATE INDEX IF NOT EXISTS ix_wallet_transactions_wallet_id ON wallet_transactions(wallet_id);
CREATE INDEX IF NOT EXISTS ix_wallet_transactions_user_id ON wallet_transactions(user_id);
