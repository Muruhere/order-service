
CREATE TABLE IF NOT EXISTS traders (
    trader_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (trader_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    trader_id VARCHAR(64) NOT NULL,
    stock VARCHAR(32) NOT NULL,
    sector VARCHAR(64) NOT NULL,
    quantity INT NOT NULL,
    side VARCHAR(4) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    version INT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_orders_trader FOREIGN KEY (trader_id) REFERENCES traders (trader_id),
    CONSTRAINT chk_orders_quantity CHECK (quantity > 0),
    CONSTRAINT chk_orders_side CHECK (side IN ('BUY', 'SELL')),
    CONSTRAINT chk_orders_status CHECK (status IN ('PENDING', 'FILLED', 'CANCELLED'))
) ENGINE=InnoDB;

CREATE INDEX idx_orders_trader_status ON orders (trader_id, status);

CREATE TABLE IF NOT EXISTS positions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    trader_id VARCHAR(64) NOT NULL,
    stock VARCHAR(32) NOT NULL,
    sector VARCHAR(64) NOT NULL,
    quantity INT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_positions_trader_stock (trader_id, stock),
    CONSTRAINT fk_positions_trader FOREIGN KEY (trader_id) REFERENCES traders (trader_id),
    CONSTRAINT chk_positions_quantity CHECK (quantity >= 0)
) ENGINE=InnoDB;
