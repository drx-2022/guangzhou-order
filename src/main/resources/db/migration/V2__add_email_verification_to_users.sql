-- Flyway Migration V2: Add Email Verification Columns to Users Table
-- Description: Add email verification support to the users table
-- Date: April 2, 2026

-- Add email_verified column (default: not verified)
ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;

-- Add verification_token column (stores the random token sent in email)
ALTER TABLE users ADD COLUMN verification_token VARCHAR(255);

-- Add verification_token_expiry column (stores token expiration time, typically 24 hours)
ALTER TABLE users ADD COLUMN verification_token_expiry TIMESTAMP;

