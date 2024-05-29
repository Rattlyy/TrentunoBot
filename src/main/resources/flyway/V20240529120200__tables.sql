CREATE TABLE IF NOT EXISTS games
(
    id          SERIAL PRIMARY KEY,
    server_id   BIGINT       NOT NULL,
    channel_id  BIGINT       NOT NULL,
    game_type   VARCHAR(100) NOT NULL,
    game_winner BIGINT
);

CREATE TABLE IF NOT EXISTS game_players
(
    id        SERIAL PRIMARY KEY,
    game_id   BIGINT NOT NULL,
    player_id BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS players
(
    id BIGINT PRIMARY KEY
)