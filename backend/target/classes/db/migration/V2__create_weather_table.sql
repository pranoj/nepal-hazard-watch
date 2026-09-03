CREATE TABLE weather (
    id BIGSERIAL PRIMARY KEY,
    location VARCHAR(255) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    temperature DOUBLE PRECISION NOT NULL,
    humidity DOUBLE PRECISION NOT NULL,
    rainfall DOUBLE PRECISION NOT NULL,
    wind_speed DOUBLE PRECISION NOT NULL,
    weather_condition VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    recorded_at TIMESTAMP NOT NULL,
    fetched_at TIMESTAMP NOT NULL,
    data_source VARCHAR(255) NOT NULL
);

CREATE INDEX idx_location_time ON weather(location, recorded_at DESC);