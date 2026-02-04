ALTER TABLE sessions
    ADD COLUMN rotated_at TIMESTAMP NULL;

-- Optional but recommended: one active session per device per user (can be relaxed later)
CREATE UNIQUE INDEX uq_sessions_user_device ON sessions(user_id, device_id);
