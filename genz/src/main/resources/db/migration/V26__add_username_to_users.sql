-- Add optional username to users for profile customization
ALTER TABLE users ADD COLUMN IF NOT EXISTS username VARCHAR(50);

CREATE UNIQUE INDEX IF NOT EXISTS ux_users_username_lower
    ON users (LOWER(username))
    WHERE username IS NOT NULL;
