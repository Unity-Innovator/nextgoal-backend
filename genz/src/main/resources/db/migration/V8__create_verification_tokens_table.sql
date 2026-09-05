CREATE TABLE IF NOT EXISTS verification_tokens (
    token_id UUID PRIMARY KEY,
    phone_number VARCHAR(30) NOT NULL,
    purpose VARCHAR(30) NOT NULL DEFAULT 'REGISTRATION',
    user_id UUID REFERENCES users(user_id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_verification_tokens_hash ON verification_tokens (token_hash);
CREATE INDEX IF NOT EXISTS ix_verification_tokens_phone ON verification_tokens (phone_number);
CREATE INDEX IF NOT EXISTS ix_verification_tokens_phone_purpose ON verification_tokens (phone_number, purpose);
CREATE INDEX IF NOT EXISTS ix_verification_tokens_user ON verification_tokens (user_id);
