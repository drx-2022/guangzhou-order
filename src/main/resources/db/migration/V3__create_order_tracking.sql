CREATE TABLE order_tracking (
    tracking_id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(order_id) ON DELETE CASCADE,
    tracking_number VARCHAR(50),
    carrier VARCHAR(100),
    current_status VARCHAR(50),
    estimated_delivery TIMESTAMP,
    actual_delivery TIMESTAMP,
    origin_location VARCHAR(255),
    destination_location VARCHAR(255),
    current_location VARCHAR(255),
    is_delivered BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE tracking_milestones (
    milestone_id BIGSERIAL PRIMARY KEY,
    tracking_id BIGINT NOT NULL REFERENCES order_tracking(tracking_id) ON DELETE CASCADE,
    step_order INTEGER NOT NULL,
    status VARCHAR(50),
    status_label VARCHAR(100),
    location VARCHAR(255),
    description TEXT,
    timestamp TIMESTAMP,
    is_completed BOOLEAN DEFAULT FALSE,
    is_current BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_order_tracking_order_id ON order_tracking(order_id);
CREATE INDEX idx_tracking_milestones_tracking_id ON tracking_milestones(tracking_id);
