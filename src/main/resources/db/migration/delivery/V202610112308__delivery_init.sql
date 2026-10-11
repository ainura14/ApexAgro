-- Schema delivery (BE3): статусы отправлений и история.
-- MODULE_SCHEMAS.md §9

CREATE SCHEMA IF NOT EXISTS delivery;

CREATE TABLE delivery.shipment_tracking (
    shipment_id            uuid         NOT NULL PRIMARY KEY,        -- logical -> ordering.shipments
    order_id               uuid         NOT NULL,                    -- logical -> ordering.orders
    buyer_id               uuid         NOT NULL,
    seller_id              uuid         NOT NULL,
    delivery_method        varchar(20)  NOT NULL CHECK (delivery_method IN ('PICKUP','SELLER_DELIVERY')),
    status                 varchar(20)  NOT NULL CHECK (status IN
                           ('CREATED','CONFIRMED','ASSEMBLING','HANDED_OVER','IN_TRANSIT',
                            'ARRIVED','DELIVERED','CANCELLED','REFUSED')),
    expected_delivery_date date,
    carrier                varchar(100),
    waybill_number         varchar(100),
    arrived_at             timestamptz,
    created_at             timestamptz  NOT NULL DEFAULT now(),
    updated_at             timestamptz  NOT NULL DEFAULT now(),
    version                int          NOT NULL DEFAULT 0
);
CREATE INDEX shipment_tracking_seller_idx  ON delivery.shipment_tracking (seller_id, status);
CREATE INDEX shipment_tracking_buyer_idx   ON delivery.shipment_tracking (buyer_id);
CREATE INDEX shipment_tracking_created_idx ON delivery.shipment_tracking (status, created_at);  -- автоотмена 48 ч
CREATE INDEX shipment_tracking_arrived_idx ON delivery.shipment_tracking (status, arrived_at);  -- авто-DELIVERED 7 дн

CREATE TABLE delivery.shipment_status_history (
    id          bigint       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    shipment_id uuid         NOT NULL REFERENCES delivery.shipment_tracking (shipment_id),
    from_status varchar(20),
    to_status   varchar(20)  NOT NULL,
    actor_id    uuid,
    actor_type  varchar(10)  NOT NULL CHECK (actor_type IN ('BUYER','SELLER','ADMIN','SYSTEM')),
    comment     varchar(500),
    changed_at  timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT status_history_admin_comment_chk CHECK (actor_type <> 'ADMIN' OR comment IS NOT NULL)
);
CREATE INDEX shipment_status_history_idx ON delivery.shipment_status_history (shipment_id, changed_at);

-- Идемпотентность обработчиков событий (E-07).
CREATE TABLE delivery.processed_events (
    event_id     uuid         NOT NULL,
    consumer     varchar(100) NOT NULL,
    processed_at timestamptz  NOT NULL DEFAULT now(),
    PRIMARY KEY (event_id, consumer)
);
