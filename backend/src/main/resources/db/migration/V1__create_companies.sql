CREATE TABLE companies (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    industry VARCHAR(120),
    location VARCHAR(160),
    website_url VARCHAR(500),
    status VARCHAR(30) NOT NULL,
    summary VARCHAR(2000),
    memo VARCHAR(5000),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT companies_status_check
        CHECK (status IN ('INTERESTED', 'PREPARING', 'APPLIED', 'ARCHIVED'))
);

CREATE INDEX companies_updated_at_idx ON companies (updated_at DESC);
