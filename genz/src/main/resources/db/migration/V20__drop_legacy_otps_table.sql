-- Migration V20: Ensure legacy otps table is dropped for deployment
-- OTP challenges are managed exclusively in Redis.
DROP TABLE IF EXISTS otps CASCADE;
