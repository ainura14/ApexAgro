-- Schema ordering (BE2): корзины, заказы, отправления (состав), идемпотентность.
-- MODULE_SCHEMAS.md §8. Статус отправления — read-model из delivery (D-15). Статусы заказа/оплаты — D-24.

CREATE SCHEMA IF NOT EXISTS ordering;

CREATE TABLE ordering.carts (
    id         uuid        NOT NULL PRIMARY KEY,
    user_id    uuid        NOT NULL UNIQUE,                          -- logical -> identity.users
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE ordering.cart_items (
    cart_id      uuid          NOT NULL REFERENCES ordering.carts (id) ON DELETE CASCADE,
    product_id   uuid          NOT NULL,                             -- logical -> catalog.products
    qty          numeric(14,3) NOT NULL CHECK (qty > 0),
    price_at_add numeric(14,2) NOT NULL,
    added_at     timestamptz   NOT NULL DEFAULT now(),
    PRIMARY KEY (cart_id, product_id)
);

CREATE TABLE ordering.orders (
    id                uuid          NOT NULL PRIMARY KEY,
    number            varchar(20)   NOT NULL UNIQUE,                 -- PROPOSED: AA-YYMMDD-NNNNN
    buyer_id          uuid          NOT NULL,                        -- logical -> identity.users
    status            varchar(20)   NOT NULL CHECK (status IN ('PLACED','IN_PROGRESS','COMPLETED','CANCELLED')),
    address_snapshot  jsonb         NOT NULL,
    payment_method    varchar(30)   NOT NULL CHECK (payment_method IN ('CASH_ON_DELIVERY','BY_AGREEMENT')),
    payment_status    varchar(20)   NOT NULL CHECK (payment_status IN ('PENDING','PAID','REFUNDED','NOT_APPLICABLE')),
    payment_reference varchar(100),
    items_total       numeric(14,2) NOT NULL CHECK (items_total >= 0),
    currency          char(3)       NOT NULL DEFAULT 'KGS',
    placed_at         timestamptz   NOT NULL DEFAULT now(),
    updated_at        timestamptz   NOT NULL DEFAULT now()
);
CREATE INDEX orders_buyer_idx  ON ordering.orders (buyer_id, placed_at DESC);
CREATE INDEX orders_status_idx ON ordering.orders (status);

CREATE TABLE ordering.shipments (
    id                uuid          NOT NULL PRIMARY KEY,
    order_id          uuid          NOT NULL REFERENCES ordering.orders (id),
    seller_id         uuid          NOT NULL,                        -- logical -> catalog.seller_profiles
    delivery_method   varchar(20)   NOT NULL CHECK (delivery_method IN ('PICKUP','SELLER_DELIVERY')),
    items_total       numeric(14,2) NOT NULL,
    commission_rate   numeric(5,4)  NOT NULL DEFAULT 0,
    commission_amount numeric(14,2) NOT NULL DEFAULT 0,
    payout_amount     numeric(14,2) NOT NULL,
    current_status    varchar(20)   NOT NULL DEFAULT 'CREATED',      -- read-model из delivery
    status_updated_at timestamptz   NOT NULL DEFAULT now()
);
CREATE INDEX shipments_order_idx  ON ordering.shipments (order_id);
CREATE INDEX shipments_seller_idx ON ordering.shipments (seller_id, status_updated_at DESC);

CREATE TABLE ordering.shipment_items (
    id             uuid          NOT NULL PRIMARY KEY,
    shipment_id    uuid          NOT NULL REFERENCES ordering.shipments (id) ON DELETE CASCADE,
    product_id     uuid          NOT NULL,                           -- logical -> catalog.products
    title_snapshot varchar(200)  NOT NULL,
    crop_id        uuid,
    unit           varchar(10)   NOT NULL,
    price          numeric(14,2) NOT NULL,
    qty            numeric(14,3) NOT NULL CHECK (qty > 0),
    line_total     numeric(14,2) NOT NULL
);
CREATE INDEX shipment_items_shipment_idx ON ordering.shipment_items (shipment_id);

CREATE TABLE ordering.idempotency_keys (
    user_id      uuid         NOT NULL,
    key          varchar(100) NOT NULL,                              -- заголовок Idempotency-Key
    request_hash char(64)     NOT NULL,
    order_id     uuid         REFERENCES ordering.orders (id),
    created_at   timestamptz  NOT NULL DEFAULT now(),
    expires_at   timestamptz  NOT NULL,                              -- PROPOSED: +24 ч
    PRIMARY KEY (user_id, key)
);
CREATE INDEX idempotency_keys_expires_idx ON ordering.idempotency_keys (expires_at);

-- Идемпотентность обработчиков событий (E-08).
CREATE TABLE ordering.processed_events (
    event_id     uuid         NOT NULL,
    consumer     varchar(100) NOT NULL,
    processed_at timestamptz  NOT NULL DEFAULT now(),
    PRIMARY KEY (event_id, consumer)
);
