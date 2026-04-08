-- Flyway Migration V3: Create Order Tracking Table
-- Description: Add order tracking functionality for fake delivery tracking
-- Date: April 8, 2026

CREATE TABLE order_trackings (
    tracking_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    location VARCHAR(500),
    description TEXT,
    is_current BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
    INDEX idx_order_id (order_id),
    INDEX idx_is_current (is_current)
);

-- Create index for efficient lookups
CREATE INDEX idx_order_tracking_created ON order_trackings(order_id, created_at DESC);
