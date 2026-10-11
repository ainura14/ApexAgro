-- Schema catalog (BE2): продавцы, товары, остатки, резервы, отзывы, избранное.
-- MODULE_SCHEMAS.md §6

CREATE SCHEMA IF NOT EXISTS catalog;

CREATE TABLE catalog.seller_profiles (
    user_id          uuid          NOT NULL PRIMARY KEY,             -- logical -> identity.users
    seller_type      varchar(20)   NOT NULL CHECK (seller_type IN ('FARMER','AGRO_COMPANY','SUPPLIER')),
    display_name     varchar(200)  NOT NULL,
    region_id        uuid          NOT NULL,                         -- logical -> reference.regions
    settlement_id    uuid,                                           -- logical -> reference.settlements
    contact_phone    varchar(16),
    verified         boolean       NOT NULL DEFAULT false,
    registered_at    timestamptz   NOT NULL DEFAULT now(),
    rating           numeric(3,2),
    reviews_count    int           NOT NULL DEFAULT 0,
    completed_orders int           NOT NULL DEFAULT 0,
    response_rate    numeric(5,2),                                   -- формула D-19
    created_at       timestamptz   NOT NULL DEFAULT now(),
    updated_at       timestamptz   NOT NULL DEFAULT now()
);

CREATE TABLE catalog.products (
    id                 uuid          NOT NULL PRIMARY KEY,
    seller_id          uuid          NOT NULL REFERENCES catalog.seller_profiles (user_id),
    category_id        uuid          NOT NULL,                       -- logical -> reference.categories
    crop_id            uuid,                                         -- logical -> reference.crops
    title              varchar(200)  NOT NULL,
    description        text,
    price              numeric(14,2) NOT NULL CHECK (price > 0),
    unit               varchar(10)   NOT NULL CHECK (unit IN ('KG','TON','BAG','LITER','PIECE')),
    min_qty            numeric(14,3) NOT NULL CHECK (min_qty > 0),
    stock_qty          numeric(14,3) NOT NULL CHECK (stock_qty >= 0),
    reserved_qty       numeric(14,3) NOT NULL DEFAULT 0,
    region_id          uuid          NOT NULL,                       -- logical -> reference.regions
    settlement_id      uuid          NOT NULL,                       -- logical -> reference.settlements
    delivery_options   varchar(20)   NOT NULL CHECK (delivery_options IN ('PICKUP','SELLER_DELIVERY','BOTH')),
    status             varchar(25)   NOT NULL
                       CHECK (status IN ('DRAFT','PENDING_MODERATION','PUBLISHED','REJECTED','UNPUBLISHED')),
    reject_reason      varchar(500),
    variety            varchar(100),
    harvest_year       smallint,
    harvest_date_from  date,
    harvest_date_to    date,
    quality_grade      varchar(50),                                  -- D-27
    origin_region_id   uuid,                                         -- logical -> reference.regions
    origin_district_id uuid,                                         -- logical -> reference.districts
    storage_conditions varchar(500),
    shelf_life_days    int           CHECK (shelf_life_days > 0),
    has_lab_analysis   boolean       NOT NULL DEFAULT false,
    search_vector      tsvector      GENERATED ALWAYS AS (
                           setweight(to_tsvector('russian'::regconfig, title), 'A')
                        || setweight(to_tsvector('simple'::regconfig,  title), 'A')
                        || setweight(to_tsvector('russian'::regconfig, coalesce(description, '')), 'B')
                        ||           to_tsvector('simple'::regconfig,  coalesce(description, ''))
                       ) STORED,
    published_at       timestamptz,
    created_at         timestamptz   NOT NULL DEFAULT now(),
    updated_at         timestamptz   NOT NULL DEFAULT now(),
    version            int           NOT NULL DEFAULT 0,
    CONSTRAINT products_reserved_chk CHECK (reserved_qty >= 0 AND reserved_qty <= stock_qty)
);
CREATE INDEX products_search_idx          ON catalog.products USING gin (search_vector);
CREATE INDEX products_status_category_idx ON catalog.products (status, category_id);
CREATE INDEX products_status_crop_idx     ON catalog.products (status, crop_id);
CREATE INDEX products_status_region_idx   ON catalog.products (status, region_id);
CREATE INDEX products_status_price_idx    ON catalog.products (status, price);
CREATE INDEX products_status_pub_idx      ON catalog.products (status, published_at DESC);
CREATE INDEX products_seller_status_idx   ON catalog.products (seller_id, status);

CREATE TABLE catalog.product_images (
    id         uuid     NOT NULL PRIMARY KEY,
    product_id uuid     NOT NULL REFERENCES catalog.products (id) ON DELETE CASCADE,
    file_id    uuid     NOT NULL,                                    -- logical -> media.files
    sort_order smallint NOT NULL CHECK (sort_order BETWEEN 0 AND 9),
    UNIQUE (product_id, sort_order)
);

CREATE TABLE catalog.stock_reservations (
    id          uuid          NOT NULL PRIMARY KEY,
    product_id  uuid          NOT NULL REFERENCES catalog.products (id),
    shipment_id uuid          NOT NULL,                              -- logical -> ordering.shipments
    qty         numeric(14,3) NOT NULL CHECK (qty > 0),
    status      varchar(20)   NOT NULL CHECK (status IN ('RESERVED','RELEASED','COMMITTED')),
    created_at  timestamptz   NOT NULL DEFAULT now(),
    updated_at  timestamptz   NOT NULL DEFAULT now(),
    UNIQUE (product_id, shipment_id)
);

CREATE TABLE catalog.crop_aliases (
    id       uuid         NOT NULL PRIMARY KEY,
    crop_id  uuid         NOT NULL,                                  -- logical -> reference.crops
    alias    varchar(100) NOT NULL,
    language varchar(2)   NOT NULL CHECK (language IN ('ky','ru'))
);
CREATE UNIQUE INDEX crop_aliases_alias_uq ON catalog.crop_aliases (lower(alias), language);

CREATE TABLE catalog.reviews (
    id          uuid          NOT NULL PRIMARY KEY,
    shipment_id uuid          NOT NULL UNIQUE,                       -- logical -> ordering.shipments
    author_id   uuid          NOT NULL,                              -- logical -> identity.users
    seller_id   uuid          NOT NULL REFERENCES catalog.seller_profiles (user_id),
    rating      smallint      NOT NULL CHECK (rating BETWEEN 1 AND 5),
    text        varchar(2000),
    status      varchar(20)   NOT NULL DEFAULT 'VISIBLE' CHECK (status IN ('VISIBLE','HIDDEN')),
    created_at  timestamptz   NOT NULL DEFAULT now(),
    updated_at  timestamptz   NOT NULL DEFAULT now()
);
CREATE INDEX reviews_seller_idx ON catalog.reviews (seller_id, status, created_at DESC);

CREATE TABLE catalog.review_products (
    review_id  uuid NOT NULL REFERENCES catalog.reviews (id) ON DELETE CASCADE,
    product_id uuid NOT NULL REFERENCES catalog.products (id),
    PRIMARY KEY (review_id, product_id)
);

CREATE TABLE catalog.favorites (
    user_id     uuid        NOT NULL,
    target_type varchar(10) NOT NULL CHECK (target_type IN ('PRODUCT','SELLER')),
    target_id   uuid        NOT NULL,
    created_at  timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, target_type, target_id)
);

CREATE TABLE catalog.seller_conversation_stats (
    conversation_id        uuid        NOT NULL PRIMARY KEY,         -- logical -> chat.conversations
    seller_id              uuid        NOT NULL REFERENCES catalog.seller_profiles (user_id),
    buyer_first_message_at timestamptz NOT NULL,
    seller_first_reply_at  timestamptz
);

-- Идемпотентность обработчиков событий (E-01, E-08, E-09).
CREATE TABLE catalog.processed_events (
    event_id     uuid         NOT NULL,
    consumer     varchar(100) NOT NULL,
    processed_at timestamptz  NOT NULL DEFAULT now(),
    PRIMARY KEY (event_id, consumer)
);
