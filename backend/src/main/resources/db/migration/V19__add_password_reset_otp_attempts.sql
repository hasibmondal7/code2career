ALTER TABLE password_reset_tokens
    ADD COLUMN attempt_count INTEGER NOT NULL DEFAULT 0;

DELETE FROM password_reset_tokens;
