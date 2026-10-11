-- Schema chat (BE1, PROPOSED): диалоги покупатель–продавец.
-- MODULE_SCHEMAS.md §11

CREATE SCHEMA IF NOT EXISTS chat;

CREATE TABLE chat.conversations (
    id              uuid        NOT NULL PRIMARY KEY,
    buyer_id        uuid        NOT NULL,
    seller_id       uuid        NOT NULL,
    product_id      uuid,                                            -- logical -> catalog.products
    order_id        uuid,                                            -- logical -> ordering.orders
    last_message_at timestamptz,
    created_at      timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT conversations_context_chk CHECK (product_id IS NOT NULL OR order_id IS NOT NULL),
    CONSTRAINT conversations_parties_chk CHECK (buyer_id <> seller_id)
);
CREATE UNIQUE INDEX conversations_product_uq ON chat.conversations (buyer_id, seller_id, product_id)
    WHERE product_id IS NOT NULL;
CREATE UNIQUE INDEX conversations_order_uq   ON chat.conversations (buyer_id, seller_id, order_id)
    WHERE order_id IS NOT NULL;
CREATE INDEX conversations_buyer_idx  ON chat.conversations (buyer_id, last_message_at DESC);
CREATE INDEX conversations_seller_idx ON chat.conversations (seller_id, last_message_at DESC);

CREATE TABLE chat.messages (
    id                 bigint        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    conversation_id    uuid          NOT NULL REFERENCES chat.conversations (id),
    sender_id          uuid          NOT NULL,
    text               varchar(2000) NOT NULL CHECK (length(text) > 0),
    attachment_file_id uuid,                                         -- logical -> media.files, в MVP не используется
    created_at         timestamptz   NOT NULL DEFAULT now()
);
CREATE INDEX messages_conversation_idx ON chat.messages (conversation_id, id DESC);

CREATE TABLE chat.conversation_reads (
    conversation_id      uuid        NOT NULL REFERENCES chat.conversations (id),
    user_id              uuid        NOT NULL,
    last_read_message_id bigint,
    updated_at           timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (conversation_id, user_id)
);
