-- Schema weather (BE3, PROPOSED): снимки погоды, пороги, предупреждения.
-- MODULE_SCHEMAS.md §13. Пороги кроме заморозка — D-22.

CREATE SCHEMA IF NOT EXISTS weather;

-- HISTORY копится с запуска: вход будущей модели урожая.
CREATE TABLE weather.weather_snapshots (
    id            uuid        NOT NULL PRIMARY KEY,
    settlement_id uuid        NOT NULL,                              -- logical -> reference.settlements
    kind          varchar(10) NOT NULL CHECK (kind IN ('CURRENT','FORECAST','HISTORY')),
    for_date      date        NOT NULL,
    payload       jsonb       NOT NULL,
    provider      varchar(30) NOT NULL,
    fetched_at    timestamptz NOT NULL DEFAULT now(),
    UNIQUE (settlement_id, kind, for_date)
);

CREATE TABLE weather.alert_thresholds (
    id              uuid         NOT NULL PRIMARY KEY,
    type            varchar(20)  NOT NULL CHECK (type IN ('FROST','HEAVY_RAIN','HEAT','WIND')),
    crop_id         uuid,                                            -- NULL = общий порог
    threshold_value numeric(8,2) NOT NULL,
    unit            varchar(10)  NOT NULL,
    active          boolean      NOT NULL DEFAULT true
);

CREATE TABLE weather.weather_alerts (
    id            uuid        NOT NULL PRIMARY KEY,
    settlement_id uuid        NOT NULL,
    type          varchar(20) NOT NULL,
    severity      varchar(10) NOT NULL CHECK (severity IN ('WARNING','CRITICAL')),
    valid_from    timestamptz NOT NULL,
    valid_to      timestamptz NOT NULL,
    details       jsonb       NOT NULL,
    created_at    timestamptz NOT NULL DEFAULT now(),
    UNIQUE (settlement_id, type, valid_from)
);
