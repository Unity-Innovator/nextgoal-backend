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
