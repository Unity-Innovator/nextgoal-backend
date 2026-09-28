-- Drop legacy Hibernate-generated check constraints on transaction tables
-- to allow WITHDRAWAL and other enum values.

ALTER TABLE IF EXISTS wallet_transactions
    DROP CONSTRAINT IF EXISTS wallet_transactions_transaction_type_check;

ALTER TABLE IF EXISTS wallet_transactions
    DROP CONSTRAINT IF EXISTS wallet_transactions_status_check;

ALTER TABLE IF EXISTS group_wallet_transactions
    DROP CONSTRAINT IF EXISTS group_wallet_transactions_transaction_type_check;

ALTER TABLE IF EXISTS group_wallet_transactions
    DROP CONSTRAINT IF EXISTS group_wallet_transactions_status_check;

ALTER TABLE IF EXISTS withdrawal_requests
    DROP CONSTRAINT IF EXISTS withdrawal_requests_status_check;
