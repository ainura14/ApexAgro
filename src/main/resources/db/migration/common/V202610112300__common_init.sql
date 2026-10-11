-- Schema common (BE3): технические таблицы.
-- MODULE_SCHEMAS.md §17

CREATE SCHEMA IF NOT EXISTS common;

-- Реестр публикации событий Spring Modulith 2.x (transactional outbox).
-- Структура задаётся библиотекой (spring-modulith-events-jpa); имена колонок не менять.
CREATE TABLE common.event_publication (
    id                     uuid        NOT NULL PRIMARY KEY,
    listener_id            text        NOT NULL,
    event_type             text        NOT NULL,
    serialized_event       text        NOT NULL,
    publication_date       timestamptz NOT NULL,
    completion_date        timestamptz,
    status                 text,
    completion_attempts    int,
    last_resubmission_date timestamptz
);
CREATE INDEX event_publication_by_completion_date_idx ON common.event_publication (completion_date);
CREATE INDEX event_publication_serialized_event_hash_idx ON common.event_publication USING hash (serialized_event);

-- ShedLock (JdbcTemplateLockProvider), блокировки задач по расписанию.
CREATE TABLE common.shedlock (
    name       varchar(64)  NOT NULL PRIMARY KEY,
    lock_until timestamptz  NOT NULL,
    locked_at  timestamptz  NOT NULL,
    locked_by  varchar(255) NOT NULL
);
