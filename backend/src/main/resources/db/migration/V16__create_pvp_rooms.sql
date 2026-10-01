CREATE TABLE pvp_rooms (
    id BIGSERIAL PRIMARY KEY,
    invite_code VARCHAR(12) NOT NULL UNIQUE,
    room_name VARCHAR(80) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    time_limit_minutes INTEGER NOT NULL,
    max_players INTEGER NOT NULL,
    problem_id BIGINT NOT NULL REFERENCES problems(id),
    host_id BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP(6),
    ends_at TIMESTAMP(6),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_pvp_room_status CHECK (status IN ('WAITING', 'IN_PROGRESS', 'FINISHED')),
    CONSTRAINT chk_pvp_room_time_limit CHECK (time_limit_minutes BETWEEN 5 AND 60),
    CONSTRAINT chk_pvp_room_max_players CHECK (max_players BETWEEN 2 AND 8)
);

CREATE INDEX idx_pvp_room_status ON pvp_rooms(status);

CREATE TABLE pvp_room_players (
    id BIGSERIAL PRIMARY KEY,
    room_id BIGINT NOT NULL REFERENCES pvp_rooms(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    joined_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_pvp_room_player UNIQUE (room_id, user_id)
);

CREATE INDEX idx_pvp_room_players_room ON pvp_room_players(room_id);
