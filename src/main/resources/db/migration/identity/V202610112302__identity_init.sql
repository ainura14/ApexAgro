-- Schema identity (BE1): пользователи, роли, токены, идентификация.
-- MODULE_SCHEMAS.md §3. OTP хранится в Redis, не здесь.

CREATE SCHEMA IF NOT EXISTS identity;

CREATE TABLE identity.users (
    id                 uuid         NOT NULL PRIMARY KEY,
    phone              varchar(16)  NOT NULL UNIQUE,                 -- E.164
    full_name          varchar(200),
    language           varchar(2)   NOT NULL DEFAULT 'ru' CHECK (language IN ('ky','ru')),  -- D-26
    verification_level varchar(20)  NOT NULL DEFAULT 'PHONE_VERIFIED'
                       CHECK (verification_level IN ('PHONE_VERIFIED','PENDING','IDENTIFIED','REJECTED')),
    status             varchar(20)  NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','BLOCKED')),
    avatar_file_id     uuid,                                         -- logical -> media.files
    created_at         timestamptz  NOT NULL DEFAULT now(),
    updated_at         timestamptz  NOT NULL DEFAULT now(),
    version            int          NOT NULL DEFAULT 0
);

-- Роль EXPERT — D-11.
CREATE TABLE identity.user_roles (
    user_id    uuid        NOT NULL REFERENCES identity.users (id) ON DELETE CASCADE,
    role       varchar(20) NOT NULL CHECK (role IN ('SELLER','MODERATOR','ADMIN')),
    granted_at timestamptz NOT NULL DEFAULT now(),
    granted_by uuid,                                                 -- NULL = система
    PRIMARY KEY (user_id, role)
);

CREATE TABLE identity.refresh_tokens (
    id          uuid         NOT NULL PRIMARY KEY,
    user_id     uuid         NOT NULL REFERENCES identity.users (id),
    family_id   uuid         NOT NULL,
    token_hash  char(64)     NOT NULL UNIQUE,                        -- SHA-256
    device      varchar(200),
    ip          inet,
    expires_at  timestamptz  NOT NULL,
    revoked_at  timestamptz,
    replaced_by uuid,
    created_at  timestamptz  NOT NULL DEFAULT now()
);
CREATE INDEX refresh_tokens_user_idx    ON identity.refresh_tokens (user_id);
CREATE INDEX refresh_tokens_family_idx  ON identity.refresh_tokens (family_id);
CREATE INDEX refresh_tokens_expires_idx ON identity.refresh_tokens (expires_at);

-- Фото документа — D-06 (при «да» добавится document_file_id).
CREATE TABLE identity.verification_requests (
    id            uuid         NOT NULL PRIMARY KEY,
    user_id       uuid         NOT NULL REFERENCES identity.users (id),
    full_name     varchar(200) NOT NULL,
    pin_encrypted bytea        NOT NULL,                             -- AES-GCM на уровне приложения
    birth_date    date         NOT NULL,
    status        varchar(20)  NOT NULL CHECK (status IN ('PENDING','APPROVED','REJECTED')),
    reviewer_id   uuid,
    reject_reason varchar(500),
    submitted_at  timestamptz  NOT NULL DEFAULT now(),
    reviewed_at   timestamptz,
    CONSTRAINT verification_reject_reason_chk
        CHECK (status <> 'REJECTED' OR reject_reason IS NOT NULL)
);
CREATE INDEX verification_requests_queue_idx ON identity.verification_requests (status, submitted_at);
CREATE UNIQUE INDEX verification_requests_one_pending_uq
    ON identity.verification_requests (user_id) WHERE status = 'PENDING';

-- Идемпотентность обработчиков событий (E-03).
CREATE TABLE identity.processed_events (
    event_id     uuid         NOT NULL,
    consumer     varchar(100) NOT NULL,
    processed_at timestamptz  NOT NULL DEFAULT now(),
    PRIMARY KEY (event_id, consumer)
);
