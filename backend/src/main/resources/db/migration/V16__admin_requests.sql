ALTER TABLE app_users
    ADD COLUMN admin_request_count_date DATE,
    ADD COLUMN admin_request_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN last_admin_request_at TIMESTAMPTZ,
    ADD CONSTRAINT ck_app_users_admin_request_count CHECK (admin_request_count BETWEEN 0 AND 50);

CREATE TABLE admin_requests (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    kind VARCHAR(30) NOT NULL CHECK (kind IN ('BUG_REPORT', 'FEATURE_REQUEST')),
    message TEXT NOT NULL CHECK (char_length(btrim(message)) BETWEEN 5 AND 2000),
    feedback TEXT CHECK (feedback IS NULL OR char_length(btrim(feedback)) BETWEEN 5 AND 2000),
    feedback_updated_at TIMESTAMPTZ,
    feedback_read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_admin_requests_owner_updated ON admin_requests(owner_id, updated_at DESC);
CREATE INDEX idx_admin_requests_created ON admin_requests(created_at DESC);
