CREATE TABLE orders (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        user_id BIGINT NOT NULL,
                        status VARCHAR(30) NOT NULL,
                        total_cents BIGINT NOT NULL DEFAULT 0,
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id),
                        INDEX idx_orders_user_id (user_id)
);

-- Optional: order events for audit-like history (not required yet)
CREATE TABLE order_events (
                              id BIGINT PRIMARY KEY AUTO_INCREMENT,
                              order_id BIGINT NOT NULL,
                              event_type VARCHAR(60) NOT NULL,
                              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              CONSTRAINT fk_order_events_order FOREIGN KEY (order_id) REFERENCES orders(id),
                              INDEX idx_order_events_order_id (order_id)
);