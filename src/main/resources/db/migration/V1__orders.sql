CREATE TABLE catalog_items (
    id BIGINT PRIMARY KEY,
    name TEXT NOT NULL,
    price_cents INTEGER NOT NULL CHECK (price_cents >= 0)
);

INSERT INTO catalog_items (id, name, price_cents) VALUES
    (1, 'Notebook', 1299),
    (2, 'Pen', 299),
    (3, 'Desk lamp', 3999);

CREATE TABLE orders (
    id UUID PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES catalog_items(id),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price_cents INTEGER NOT NULL CHECK (unit_price_cents >= 0),
    status TEXT NOT NULL CHECK (status IN ('PENDING', 'PROCESSED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE order_outbox (
    order_id UUID PRIMARY KEY REFERENCES orders(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ
);

CREATE INDEX order_outbox_pending_idx ON order_outbox (created_at) WHERE published_at IS NULL;
