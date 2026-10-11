-- Schema integration (BE3): реестр внешних источников данных.
-- MODULE_SCHEMAS.md §14

CREATE SCHEMA IF NOT EXISTS integration;

CREATE TABLE integration.integration_sources (
    id              uuid         NOT NULL PRIMARY KEY,
    code            varchar(50)  NOT NULL UNIQUE,
    name            varchar(200) NOT NULL,
    data_kind       varchar(20)  NOT NULL CHECK (data_kind IN ('PRICES','WEATHER','OTHER')),
    status          varchar(20)  NOT NULL CHECK (status IN ('ACTIVE','DEGRADED','DISABLED')),
    reliability     varchar(20)  NOT NULL CHECK (reliability IN ('OFFICIAL_API','CONTRACT','PUBLIC_DATA','MANUAL')),
    has_agreement   boolean      NOT NULL DEFAULT false,
    last_updated_at timestamptz,
    notes           varchar(500)
);
