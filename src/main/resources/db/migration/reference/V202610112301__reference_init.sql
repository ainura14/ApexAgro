-- Schema reference (BE1, PROPOSED): справочники.
-- MODULE_SCHEMAS.md §2. Seed — отдельными R__reference_seed_*.sql (D-16).

CREATE SCHEMA IF NOT EXISTS reference;

CREATE TABLE reference.regions (
    id         uuid         NOT NULL PRIMARY KEY,
    code       varchar(20)  NOT NULL UNIQUE,
    name_ky    varchar(100) NOT NULL,
    name_ru    varchar(100) NOT NULL,
    sort_order int          NOT NULL DEFAULT 0
);

CREATE TABLE reference.districts (
    id        uuid         NOT NULL PRIMARY KEY,
    region_id uuid         NOT NULL REFERENCES reference.regions (id),
    code      varchar(20)  NOT NULL UNIQUE,
    name_ky   varchar(100) NOT NULL,
    name_ru   varchar(100) NOT NULL
);
CREATE INDEX districts_region_idx ON reference.districts (region_id);

CREATE TABLE reference.settlements (
    id          uuid         NOT NULL PRIMARY KEY,
    district_id uuid         NOT NULL REFERENCES reference.districts (id),
    code        varchar(30)  NOT NULL UNIQUE,
    name_ky     varchar(150) NOT NULL,
    name_ru     varchar(150) NOT NULL,
    lat         numeric(9,6) NOT NULL CHECK (lat BETWEEN -90 AND 90),
    lon         numeric(9,6) NOT NULL CHECK (lon BETWEEN -180 AND 180)
);
CREATE INDEX settlements_district_idx ON reference.settlements (district_id);
CREATE INDEX settlements_name_ru_idx  ON reference.settlements (lower(name_ru));
CREATE INDEX settlements_name_ky_idx  ON reference.settlements (lower(name_ky));

CREATE TABLE reference.crops (
    id      uuid         NOT NULL PRIMARY KEY,
    code    varchar(50)  NOT NULL UNIQUE,
    name_ky varchar(100) NOT NULL,
    name_ru varchar(100) NOT NULL,
    active  boolean      NOT NULL DEFAULT true
);

-- Коэффициенты пересчёта в кг; KG = 1 неявно.
CREATE TABLE reference.crop_unit_factors (
    crop_id     uuid          NOT NULL REFERENCES reference.crops (id),
    unit        varchar(10)   NOT NULL CHECK (unit IN ('TON','BAG','LITER','PIECE')),
    kg_per_unit numeric(10,3) NOT NULL CHECK (kg_per_unit > 0),
    PRIMARY KEY (crop_id, unit)
);

CREATE TABLE reference.categories (
    id         uuid         NOT NULL PRIMARY KEY,
    parent_id  uuid         REFERENCES reference.categories (id),
    level      smallint     NOT NULL CHECK (level BETWEEN 1 AND 3),
    code       varchar(50)  NOT NULL UNIQUE,
    name_ky    varchar(100) NOT NULL,
    name_ru    varchar(100) NOT NULL,
    kind       varchar(20)  NOT NULL CHECK (kind IN ('HARVEST','AGRO_GOODS')),
    sort_order int          NOT NULL DEFAULT 0,
    active     boolean      NOT NULL DEFAULT true
);
CREATE INDEX categories_parent_idx ON reference.categories (parent_id);
