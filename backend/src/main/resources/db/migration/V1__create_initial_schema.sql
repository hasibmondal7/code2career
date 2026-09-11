CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    xp INTEGER NOT NULL DEFAULT 0,
    level INTEGER NOT NULL DEFAULT 1,
    current_streak INTEGER NOT NULL DEFAULT 0,
    last_active_date DATE,
    created_at TIMESTAMP(6)
);

CREATE TABLE IF NOT EXISTS topics (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(255),
    admin_id BIGINT REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS problems (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    description TEXT NOT NULL,
    difficulty VARCHAR(20) NOT NULL,
    constraints TEXT,
    xp_reward INTEGER,
    topic_id BIGINT NOT NULL REFERENCES topics(id),
    admin_id BIGINT REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS test_cases (
    id BIGSERIAL PRIMARY KEY,
    input_data TEXT NOT NULL,
    expected_output TEXT NOT NULL,
    is_hidden BOOLEAN NOT NULL,
    problem_id BIGINT NOT NULL REFERENCES problems(id)
);

CREATE TABLE IF NOT EXISTS submissions (
    id BIGSERIAL PRIMARY KEY,
    code TEXT NOT NULL,
    language VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    submitted_at TIMESTAMP(6),
    problem_id BIGINT NOT NULL REFERENCES problems(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    execution_time_ms BIGINT
);

CREATE INDEX IF NOT EXISTS idx_problem_topic ON problems(topic_id);
CREATE INDEX IF NOT EXISTS idx_problem_difficulty ON problems(difficulty);
CREATE INDEX IF NOT EXISTS idx_submission_problem ON submissions(problem_id);
CREATE INDEX IF NOT EXISTS idx_submission_user ON submissions(user_id);
CREATE INDEX IF NOT EXISTS idx_submission_status ON submissions(status);
