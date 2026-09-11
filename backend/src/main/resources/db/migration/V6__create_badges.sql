CREATE TABLE IF NOT EXISTS badges (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS user_badges (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    badge_id BIGINT NOT NULL REFERENCES badges(id),
    awarded_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_user_badge UNIQUE (user_id, badge_id)
);

INSERT INTO badges (code, name, description)
VALUES
    ('FIRST_CODE', 'First Code', 'Submit your first accepted solution'),
    ('SEVEN_DAY_STREAK', '7 Day Streak', 'Code for seven consecutive days'),
    ('TEN_PROBLEMS', 'Problem Solver', 'Solve ten coding problems'),
    ('HUNDRED_XP', 'Century', 'Earn one hundred XP')
ON CONFLICT (code) DO NOTHING;
