-- Schema profile (BE1): хозяйства, поля, посевы, адреса.
-- MODULE_SCHEMAS.md §4. Ссылки на identity/reference — logical uuid без FK.

CREATE SCHEMA IF NOT EXISTS profile;

CREATE TABLE profile.farms (
    id            uuid         NOT NULL PRIMARY KEY,
    owner_id      uuid         NOT NULL,                             -- logical -> identity.users
    name          varchar(200),
    type          varchar(30)  NOT NULL CHECK (type IN ('PERSONAL_SUBSIDIARY','PEASANT_FARM','COMPANY')),
    settlement_id uuid         NOT NULL,                             -- logical -> reference.settlements
    created_at    timestamptz  NOT NULL DEFAULT now(),
    updated_at    timestamptz  NOT NULL DEFAULT now()
);
CREATE INDEX farms_owner_idx ON profile.farms (owner_id);

CREATE TABLE profile.fields (
    id            uuid          NOT NULL PRIMARY KEY,
    farm_id       uuid          NOT NULL REFERENCES profile.farms (id) ON DELETE CASCADE,
    name          varchar(100)  NOT NULL,
    area_ha       numeric(10,2) NOT NULL CHECK (area_ha > 0),
    settlement_id uuid          NOT NULL,                            -- logical -> reference.settlements
    irrigation    varchar(20)   NOT NULL CHECK (irrigation IN ('RAINFED','FURROW','DRIP','SPRINKLER')),
    soil_type     varchar(100),
    created_at    timestamptz   NOT NULL DEFAULT now(),
    updated_at    timestamptz   NOT NULL DEFAULT now()
);
CREATE INDEX fields_farm_idx       ON profile.fields (farm_id);
CREATE INDEX fields_settlement_idx ON profile.fields (settlement_id);

CREATE TABLE profile.plantings (
    id                    uuid          NOT NULL PRIMARY KEY,
    field_id              uuid          NOT NULL REFERENCES profile.fields (id) ON DELETE CASCADE,
    crop_id               uuid          NOT NULL,                    -- logical -> reference.crops
    variety               varchar(100),
    season_year           smallint      NOT NULL,
    sowing_date           date          NOT NULL,
    expected_harvest_date date,
    actual_yield_t        numeric(12,3),                             -- факт урожая: будущий датасет
    created_at            timestamptz   NOT NULL DEFAULT now(),
    updated_at            timestamptz   NOT NULL DEFAULT now()
);
CREATE INDEX plantings_field_season_idx ON profile.plantings (field_id, season_year);

CREATE TABLE profile.addresses (
    id            uuid         NOT NULL PRIMARY KEY,
    user_id       uuid         NOT NULL,                             -- logical -> identity.users
    settlement_id uuid         NOT NULL,                             -- logical -> reference.settlements
    street        varchar(200),
    house         varchar(50),
    comment       varchar(500),
    contact_phone varchar(16)  NOT NULL,
    is_default    boolean      NOT NULL DEFAULT false,
    created_at    timestamptz  NOT NULL DEFAULT now(),
    updated_at    timestamptz  NOT NULL DEFAULT now()
);
CREATE INDEX addresses_user_idx ON profile.addresses (user_id);
CREATE UNIQUE INDEX addresses_one_default_uq ON profile.addresses (user_id) WHERE is_default;
