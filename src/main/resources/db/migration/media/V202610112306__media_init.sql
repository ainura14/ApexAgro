-- Schema media (BE2, PROPOSED): файлы и варианты.
-- MODULE_SCHEMAS.md §7. Размеры вариантов — D-25.

CREATE SCHEMA IF NOT EXISTS media;

CREATE TABLE media.files (
    id           uuid         NOT NULL PRIMARY KEY,
    owner_id     uuid         NOT NULL,                              -- logical -> identity.users
    purpose      varchar(30)  NOT NULL CHECK (purpose IN
                 ('PRODUCT_IMAGE','AVATAR','CHAT_ATTACHMENT','SOIL_LAB_REPORT','SOIL_REPORT','AI_IMAGE')),
    storage_key  varchar(300) NOT NULL UNIQUE,
    content_type varchar(100) NOT NULL,
    size_bytes   bigint       NOT NULL CHECK (size_bytes > 0 AND size_bytes <= 10485760),
    status       varchar(20)  NOT NULL CHECK (status IN ('PENDING_UPLOAD','UPLOADED','PROCESSED','FAILED')),
    created_at   timestamptz  NOT NULL DEFAULT now(),
    updated_at   timestamptz  NOT NULL DEFAULT now()
);
CREATE INDEX files_owner_idx  ON media.files (owner_id, created_at);
CREATE INDEX files_status_idx ON media.files (status, created_at);

CREATE TABLE media.file_variants (
    file_id     uuid         NOT NULL REFERENCES media.files (id) ON DELETE CASCADE,
    variant     varchar(20)  NOT NULL CHECK (variant IN ('THUMB','CARD','FULL','AI_1024')),
    storage_key varchar(300) NOT NULL UNIQUE,
    width       int          NOT NULL,
    height      int          NOT NULL,
    size_bytes  bigint       NOT NULL,
    PRIMARY KEY (file_id, variant)
);
