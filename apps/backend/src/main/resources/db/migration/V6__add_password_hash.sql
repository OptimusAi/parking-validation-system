-- V6: Add optional password_hash column to app_user for local/dev authentication.
-- In production, all logins are via the external OAuth2 provider (password_hash stays null).
-- For local development (SPRING_PROFILES_ACTIVE=local), LocalDataSeeder stores BCrypt hashes
-- and AuthController verifies them directly (no OAuth server required).

ALTER TABLE app_user ADD COLUMN IF NOT EXISTS password_hash VARCHAR(256);
