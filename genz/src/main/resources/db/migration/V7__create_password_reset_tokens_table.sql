CREATE TABLE IF NOT EXISTS password_reset_tokens (
    token_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_password_reset_tokens_hash ON password_reset_tokens (token_hash);
CREATE INDEX IF NOT EXISTS ix_password_reset_tokens_user ON password_reset_tokens (user_id);
