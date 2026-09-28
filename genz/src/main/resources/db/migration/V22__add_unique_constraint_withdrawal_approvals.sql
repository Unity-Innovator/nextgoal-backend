-- Add unique constraint to ensure a user cannot have multiple approval records for the same withdrawal request
CREATE UNIQUE INDEX IF NOT EXISTS uk_withdrawal_approvals_request_user ON withdrawal_approvals (request_id, user_id);
