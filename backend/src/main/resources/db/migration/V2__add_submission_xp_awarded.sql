ALTER TABLE submissions
    ADD COLUMN IF NOT EXISTS xp_awarded BOOLEAN DEFAULT FALSE;

UPDATE submissions
SET xp_awarded = FALSE
WHERE xp_awarded IS NULL;
