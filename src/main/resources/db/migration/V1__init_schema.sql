CREATE TABLE users
(
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name            VARCHAR(64) NOT NULL UNIQUE,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    max_slots       INTEGER DEFAULT 1,
    is_bot          BOOLEAN DEFAULT FALSE,
    is_bot_active   BOOLEAN DEFAULT FALSE,
    version         BIGINT DEFAULT 0 NOT NULL,
    created_at      TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE user_balances
(
    user_id         BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    balance         NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    version         BIGINT DEFAULT 0 NOT NULL,
    created_at      TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL
);

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

CREATE SEQUENCE garage_slots_id_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE garage_slots
(
    id              BIGINT DEFAULT nextval('garage_slots_id_seq') PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    slot_number     INTEGER NOT NULL,
    car_id          UUID REFERENCES cars(id) ON DELETE SET NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'EMPTY',
    created_at      TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT      uk_user_slot_number UNIQUE (user_id, slot_number),
    CONSTRAINT      check_garage_slot_status CHECK (status IN ('EMPTY', 'OCCUPIED'))
);

CREATE TABLE market_listings
(
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    car_id          UUID NOT NULL REFERENCES cars(id) ON DELETE RESTRICT,
    seller_id       BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    buyer_id        BIGINT REFERENCES users(id) ON DELETE RESTRICT,
    price           NUMERIC(15, 2) NOT NULL DEFAULT 0.00 CHECK (price >= 0),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at       TIMESTAMPTZ,

    CONSTRAINT check_market_listing_status CHECK (status IN ('ACTIVE', 'SOLD', 'CANCELLED')),
    CONSTRAINT check_closed_at CHECK (
        (status = 'ACTIVE' AND closed_at IS NULL) OR
        (status IN ('SOLD', 'CANCELLED') AND closed_at IS NOT NULL)
        ),
    CONSTRAINT check_anti_self_buy CHECK (buyer_id IS NULL OR seller_id <> buyer_id)
);

CREATE TABLE bot_config
(
    id              INTEGER PRIMARY KEY DEFAULT 1,
    total_bots      INTEGER DEFAULT 0 NOT NULL,
    CONSTRAINT      check_single_row CHECK (id = 1)
);

ALTER SEQUENCE garage_slots_id_seq OWNED BY garage_slots.id;

CREATE INDEX idx_garage_slots_user_status ON garage_slots (user_id, status);
CREATE UNIQUE INDEX idx_garage_slots_car_unique ON garage_slots (car_id) WHERE car_id IS NOT NULL;

CREATE INDEX idx_market_listings_feed ON market_listings (created_at DESC) WHERE status = 'ACTIVE';
CREATE UNIQUE INDEX idx_market_listings_one_active_car ON market_listings (car_id) WHERE status = 'ACTIVE';

INSERT INTO bot_config (id, total_bots) VALUES (1, 0);