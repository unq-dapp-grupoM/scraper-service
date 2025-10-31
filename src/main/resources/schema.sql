-- Tabla: players
CREATE TABLE IF NOT EXISTS players (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    current_team VARCHAR(255),
    shirt_number VARCHAR(10),
    age VARCHAR(50),
    height VARCHAR(50),
    nationality VARCHAR(100),
    positions VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabla: teams
CREATE TABLE IF NOT EXISTS teams (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabla: team_players
CREATE TABLE IF NOT EXISTS team_players (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    age VARCHAR(50),
    position VARCHAR(100),
    height VARCHAR(50),
    weight VARCHAR(50),
    apps VARCHAR(50),
    mins_played VARCHAR(50),
    goals VARCHAR(50),
    assists VARCHAR(50),
    yellow_cards VARCHAR(50),
    red_cards VARCHAR(50),
    shots_per_game VARCHAR(50),
    pass_success VARCHAR(50),
    aerials_won_per_game VARCHAR(50),
    man_of_the_match VARCHAR(50),
    rating VARCHAR(50),
    team_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE
);

-- Tabla: player_match_stats
CREATE TABLE IF NOT EXISTS player_match_stats (
    id BIGSERIAL PRIMARY KEY,
    opponent VARCHAR(255),
    score VARCHAR(100),
    date VARCHAR(50),
    position VARCHAR(100),
    mins_played VARCHAR(50),
    goals VARCHAR(50),
    assists VARCHAR(50),
    yellow_cards VARCHAR(50),
    red_cards VARCHAR(50),
    shots VARCHAR(50),
    pass_success VARCHAR(50),
    aerials_won VARCHAR(50),
    rating VARCHAR(50),
    player_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (player_id) REFERENCES players(id) ON DELETE CASCADE
);

-- Tabla: match_statistics
CREATE TABLE IF NOT EXISTS match_statistics (
    id BIGSERIAL PRIMARY KEY,
    player_name VARCHAR(255) NOT NULL,
    opponent VARCHAR(255),
    match_date DATE,
    result VARCHAR(10),
    position VARCHAR(50),
    minutes_played INTEGER DEFAULT 0,
    goals INTEGER DEFAULT 0,
    assists INTEGER DEFAULT 0,
    yellow_cards INTEGER DEFAULT 0,
    red_cards INTEGER DEFAULT 0,
    shots INTEGER DEFAULT 0,
    pass_accuracy DOUBLE PRECISION DEFAULT 0.0,
    aerial_duels INTEGER DEFAULT 0,
    rating DOUBLE PRECISION DEFAULT 0.0,
    league VARCHAR(100),
    season VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabla: performance_metrics
CREATE TABLE IF NOT EXISTS performance_metrics (
    id BIGSERIAL PRIMARY KEY,
    player_name VARCHAR(255) NOT NULL,
    analysis_date DATE,
    goals_per_match DOUBLE PRECISION DEFAULT 0.0,
    assists_per_match DOUBLE PRECISION DEFAULT 0.0,
    goal_involvement DOUBLE PRECISION DEFAULT 0.0,
    shots_per_match DOUBLE PRECISION DEFAULT 0.0,
    shot_accuracy DOUBLE PRECISION DEFAULT 0.0,
    pass_accuracy DOUBLE PRECISION DEFAULT 0.0,
    key_passes_per_match DOUBLE PRECISION DEFAULT 0.0,
    aerial_duels_won DOUBLE PRECISION DEFAULT 0.0,
    recoveries_per_match DOUBLE PRECISION DEFAULT 0.0,
    average_rating DOUBLE PRECISION DEFAULT 0.0,
    rating_deviation DOUBLE PRECISION DEFAULT 0.0,
    minutes_per_match DOUBLE PRECISION DEFAULT 0.0,
    offensive_impact DOUBLE PRECISION DEFAULT 0.0,
    performance_trend DOUBLE PRECISION DEFAULT 0.0,
    goal_probability DOUBLE PRECISION DEFAULT 0.0,
    assist_probability DOUBLE PRECISION DEFAULT 0.0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabla: predictive_analysis
CREATE TABLE IF NOT EXISTS predictive_analysis (
    id BIGSERIAL PRIMARY KEY,
    player_name VARCHAR(255) NOT NULL,
    analysis_date DATE,
    goal_probability DOUBLE PRECISION DEFAULT 0.0,
    assist_probability DOUBLE PRECISION DEFAULT 0.0,
    high_rating_probability DOUBLE PRECISION DEFAULT 0.0,
    full_match_probability DOUBLE PRECISION DEFAULT 0.0,
    home_advantage_factor DOUBLE PRECISION DEFAULT 1.0,
    opponent_factor DOUBLE PRECISION DEFAULT 1.0,
    position_factor DOUBLE PRECISION DEFAULT 1.0,
    trend_factor DOUBLE PRECISION DEFAULT 0.0,
    performance_prediction VARCHAR(50),
    predictive_score DOUBLE PRECISION DEFAULT 0.0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabla: matches
CREATE TABLE IF NOT EXISTS matches (
    id BIGSERIAL PRIMARY KEY,
    home_team VARCHAR(255),
    away_team VARCHAR(255),
    date VARCHAR(50),
    competition VARCHAR(255),
    team_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE
);

-- Tabla: _user
CREATE TABLE IF NOT EXISTS _user (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
);