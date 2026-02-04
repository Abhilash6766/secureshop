ALTER TABLE audit_log
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS';

CREATE INDEX idx_audit_status ON audit_log(status);
