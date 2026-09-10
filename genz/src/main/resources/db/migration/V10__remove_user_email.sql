-- Email is no longer part of authentication or the user profile.
DROP INDEX IF EXISTS ux_users_email_lower;
ALTER TABLE users DROP COLUMN IF EXISTS email;
