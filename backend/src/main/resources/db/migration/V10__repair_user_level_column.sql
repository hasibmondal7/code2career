DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'users'
          AND column_name = 'user_level'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'users'
          AND column_name = 'level'
    ) THEN
        ALTER TABLE users RENAME COLUMN user_level TO level;
    ELSIF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'users'
          AND column_name = 'user_level'
    ) AND EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'users'
          AND column_name = 'level'
    ) THEN
        UPDATE users
        SET level = COALESCE(level, user_level, 1);

        ALTER TABLE users DROP COLUMN user_level;
    ELSIF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'users'
          AND column_name = 'level'
    ) THEN
        ALTER TABLE users ADD COLUMN level INTEGER;
    END IF;
END
$$;

UPDATE users
SET level = COALESCE(level, 1);

ALTER TABLE users
    ALTER COLUMN level SET DEFAULT 1,
    ALTER COLUMN level SET NOT NULL;
