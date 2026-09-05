-- Existing login OTPs must not be reusable for registration or password reset.
UPDATE otps SET used = TRUE WHERE used = FALSE;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS email VARCHAR(254);

CREATE UNIQUE INDEX IF NOT EXISTS ux_users_email_lower
    ON users (LOWER(email))
    WHERE email IS NOT NULL;

ALTER TABLE otps
    ADD COLUMN IF NOT EXISTS purpose VARCHAR(30);

UPDATE otps
SET purpose = 'REGISTRATION'
WHERE purpose IS NULL;

ALTER TABLE otps
    ALTER COLUMN purpose SET NOT NULL;

DROP INDEX IF EXISTS ix_otps_phone_created;

CREATE INDEX IF NOT EXISTS ix_otps_phone_purpose_created
    ON otps (phone_number, purpose, created_at DESC);

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
