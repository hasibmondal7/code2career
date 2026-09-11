DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'users'
          AND column_name = 'total_xp'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'users'
          AND column_name = 'xp'
    ) THEN
        ALTER TABLE users RENAME COLUMN total_xp TO xp;
    ELSIF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'users'
          AND column_name = 'total_xp'
    ) AND EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'users'
          AND column_name = 'xp'
    ) THEN
        UPDATE users
        SET xp = COALESCE(xp, total_xp, 0);

        ALTER TABLE users DROP COLUMN total_xp;
    ELSIF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'users'
          AND column_name = 'xp'
    ) THEN
        ALTER TABLE users ADD COLUMN xp INTEGER;
    END IF;
END
$$;

UPDATE users
SET xp = COALESCE(xp, 0);

ALTER TABLE users
    ALTER COLUMN xp SET DEFAULT 0,
    ALTER COLUMN xp SET NOT NULL;
