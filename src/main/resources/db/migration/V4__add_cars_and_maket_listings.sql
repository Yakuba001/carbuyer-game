CREATE TABLE cars
(
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vin             VARCHAR(17) UNIQUE NOT NULL,
    model           VARCHAR(64) NOT NULL,
    mileage         INTEGER NOT NULL DEFAULT 0 CHECK (mileage >= 0),
    condition_score INTEGER NOT NULL CHECK (condition_score BETWEEN 0 AND 100),
    real_price      NUMERIC(15, 2) NOT NULL DEFAULT 0.00 CHECK (real_price >= 0),
    owner_id        BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_cars_owner_id ON cars(owner_id);

CREATE TABLE market_listings
(
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    car_id      UUID NOT NULL REFERENCES cars(id) ON DELETE RESTRICT,
    seller_id   BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    buyer_id    BIGINT REFERENCES users(id) ON DELETE RESTRICT,
    price       NUMERIC(15, 2) NOT NULL DEFAULT 0.00 CHECK (price >= 0),
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at   TIMESTAMPTZ,

    CONSTRAINT check_market_listing_status CHECK (status IN ('ACTIVE', 'SOLD', 'CANCELLED')),
    CONSTRAINT check_closed_at CHECK (
        (status = 'ACTIVE' AND closed_at IS NULL) OR
        (status IN ('SOLD', 'CANCELLED') AND closed_at IS NOT NULL)
    ),
    CONSTRAINT check_anti_self_buy CHECK (buyer_id IS NULL OR seller_id <> buyer_id)
);

CREATE INDEX idx_market_listings_car_id ON market_listings(car_id);
CREATE INDEX idx_market_listings_seller_id ON market_listings(seller_id);
CREATE INDEX idx_market_listings_buyer_id ON market_listings(buyer_id) WHERE buyer_id IS NOT NULL;

CREATE INDEX idx_market_listings_feed
    ON market_listings(created_at DESC)
    WHERE status = 'ACTIVE';

CREATE UNIQUE INDEX idx_market_listings_one_active_car
    ON market_listings(car_id)
    WHERE status = 'ACTIVE';

ALTER TABLE garage_slots
    ALTER COLUMN car_id TYPE UUID USING car_id::text::uuid;

ALTER TABLE garage_slots
    ADD CONSTRAINT fk_garage_slots_car_id
    FOREIGN KEY (car_id) REFERENCES cars(id) ON DELETE SET NULL;

ALTER TABLE garage_slots
    ADD CONSTRAINT check_garage_slot_status
    CHECK (status IN ('EMPTY', 'OCCUPIED'));

CREATE INDEX idx_garage_slots_car_id
    ON garage_slots(car_id)
    WHERE car_id IS NOT NULL;