-- Schema audit (BE1, PROPOSED): журнал аудита. Только INSERT.
-- MODULE_SCHEMAS.md §5

CREATE SCHEMA IF NOT EXISTS audit;

CREATE TABLE audit.audit_log (
    id             bigint       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    actor_id       uuid,                                             -- NULL = система
    actor_roles    varchar(100),
    action         varchar(100) NOT NULL,
    entity_type    varchar(50)  NOT NULL,
    entity_id      varchar(100) NOT NULL,
    diff           jsonb,                                            -- без ПИН и секретов
    ip             inet,
    correlation_id varchar(64),
    created_at     timestamptz  NOT NULL DEFAULT now()
);
CREATE INDEX audit_log_entity_idx  ON audit.audit_log (entity_type, entity_id);
CREATE INDEX audit_log_actor_idx   ON audit.audit_log (actor_id, created_at);
CREATE INDEX audit_log_created_idx ON audit.audit_log (created_at);
