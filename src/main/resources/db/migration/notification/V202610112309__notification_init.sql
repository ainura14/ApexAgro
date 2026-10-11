-- Schema notification (BE3): уведомления, каналы, push-подписки, SMS-лог.
-- MODULE_SCHEMAS.md §10

CREATE SCHEMA IF NOT EXISTS notification;

CREATE TABLE notification.notifications (
    id              uuid        NOT NULL PRIMARY KEY,
    recipient_id    uuid        NOT NULL,                            -- logical -> identity.users
    type            varchar(40) NOT NULL CHECK (type IN
                    ('ORDER_NEW','SHIPMENT_STATUS','CHAT_MESSAGE','VERIFICATION_RESULT','WEATHER_ALERT')),
    template_code   varchar(80) NOT NULL,
    payload         jsonb       NOT NULL,
    source_event_id uuid,
    read_at         timestamptz,
    created_at      timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT notifications_source_uq UNIQUE (source_event_id, recipient_id)
);
CREATE INDEX notifications_recipient_idx ON notification.notifications (recipient_id, created_at DESC);
CREATE INDEX notifications_unread_idx    ON notification.notifications (recipient_id) WHERE read_at IS NULL;

CREATE TABLE notification.notification_deliveries (
    id              uuid         NOT NULL PRIMARY KEY,
    notification_id uuid         NOT NULL REFERENCES notification.notifications (id) ON DELETE CASCADE,
    channel         varchar(10)  NOT NULL CHECK (channel IN ('PUSH','SMS')),
    status          varchar(10)  NOT NULL CHECK (status IN ('PENDING','SENT','FAILED','SKIPPED')),
    attempts        smallint     NOT NULL DEFAULT 0,
    last_error      varchar(500),
    sent_at         timestamptz,
    UNIQUE (notification_id, channel)
);

CREATE TABLE notification.notification_templates (
    code     varchar(80)  NOT NULL,
    channel  varchar(10)  NOT NULL CHECK (channel IN ('IN_APP','PUSH','SMS')),
    language varchar(2)   NOT NULL CHECK (language IN ('ky','ru')),
    title    varchar(200),
    body     text         NOT NULL,
    PRIMARY KEY (code, channel, language)
);

CREATE TABLE notification.push_subscriptions (
    id           uuid         NOT NULL PRIMARY KEY,
    user_id      uuid         NOT NULL,                              -- logical -> identity.users
    endpoint     text         NOT NULL UNIQUE,
    p256dh       varchar(200) NOT NULL,
    auth         varchar(100) NOT NULL,
    user_agent   varchar(300),
    created_at   timestamptz  NOT NULL DEFAULT now(),
    last_used_at timestamptz
);
CREATE INDEX push_subscriptions_user_idx ON notification.push_subscriptions (user_id);

-- Номер целиком и текст OTP не хранятся.
CREATE TABLE notification.sms_log (
    id                  uuid         NOT NULL PRIMARY KEY,
    phone_masked        varchar(20)  NOT NULL,
    purpose             varchar(20)  NOT NULL CHECK (purpose IN ('OTP','NOTIFICATION')),
    status              varchar(10)  NOT NULL CHECK (status IN ('SENT','FAILED')),
    provider_message_id varchar(100),
    created_at          timestamptz  NOT NULL DEFAULT now()
);
CREATE INDEX sms_log_created_idx ON notification.sms_log (created_at);

-- Идемпотентность обработчиков событий (E-01, E-02, E-07, E-08, E-09, E-10).
CREATE TABLE notification.processed_events (
    event_id     uuid         NOT NULL,
    consumer     varchar(100) NOT NULL,
    processed_at timestamptz  NOT NULL DEFAULT now(),
    PRIMARY KEY (event_id, consumer)
);
