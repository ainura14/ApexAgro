-- Schema analytics (BE3): наблюдения цен и агрегаты.
-- MODULE_SCHEMAS.md §12. «Сезон» — D-21.

CREATE SCHEMA IF NOT EXISTS analytics;

CREATE TABLE analytics.price_observations (
    id             uuid          NOT NULL PRIMARY KEY,
    crop_id        uuid          NOT NULL,                           -- logical -> reference.crops
    region_id      uuid          NOT NULL,                           -- logical -> reference.regions
    price_per_kg   numeric(14,4) NOT NULL CHECK (price_per_kg > 0),
    original_price numeric(14,2) NOT NULL,
    original_unit  varchar(10)   NOT NULL,
    observed_at    timestamptz   NOT NULL,
    source_type    varchar(20)   NOT NULL CHECK (source_type IN ('PLATFORM','MANUAL','EXTERNAL')),
    source_ref     varchar(200)  NOT NULL,
    source_note    varchar(300),
    is_demo        boolean       NOT NULL DEFAULT false,
    created_by     uuid,
    created_at     timestamptz   NOT NULL DEFAULT now(),
    CONSTRAINT price_observations_source_uq UNIQUE (source_ref, observed_at),
    CONSTRAINT price_observations_manual_note_chk CHECK (source_type <> 'MANUAL' OR source_note IS NOT NULL)
);
CREATE INDEX price_observations_lookup_idx ON analytics.price_observations (crop_id, region_id, observed_at);
CREATE INDEX price_observations_demo_idx   ON analytics.price_observations (is_demo);

CREATE TABLE analytics.price_aggregates (
    crop_id             uuid          NOT NULL,
    region_id           uuid          NOT NULL,
    period              varchar(10)   NOT NULL CHECK (period IN ('D7','D30','SEASON')),
    calculated_for      date          NOT NULL,
    avg_price           numeric(14,4),
    median_price        numeric(14,4),
    min_price           numeric(14,4),
    max_price           numeric(14,4),
    observations_count  int           NOT NULL,
    insufficient_data   boolean       NOT NULL,
    change_pct          numeric(7,2),
    last_observation_at timestamptz,
    computed_at         timestamptz   NOT NULL DEFAULT now(),
    PRIMARY KEY (crop_id, region_id, period, calculated_for)
);

-- Идемпотентность обработчиков событий (E-05, E-06, E-08).
CREATE TABLE analytics.processed_events (
    event_id     uuid         NOT NULL,
    consumer     varchar(100) NOT NULL,
    processed_at timestamptz  NOT NULL DEFAULT now(),
    PRIMARY KEY (event_id, consumer)
);
