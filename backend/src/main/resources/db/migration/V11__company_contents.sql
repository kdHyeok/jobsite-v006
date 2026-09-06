-- 기업별 뉴스 기사와 유튜브 영상을 같은 앨범에서 관리한다 — docs/company-content.md.
CREATE TABLE company_contents (
    id         UUID PRIMARY KEY,
    owner_id   UUID         NOT NULL REFERENCES app_users (id) ON DELETE CASCADE,
    company_id UUID         NOT NULL REFERENCES companies (id) ON DELETE CASCADE,
    kind       VARCHAR(20)  NOT NULL,
    title      VARCHAR(200) NOT NULL,
    preview    TEXT,
    source     VARCHAR(160),
    url        VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL,
    CONSTRAINT company_contents_kind_check CHECK (kind IN ('NEWS', 'YOUTUBE'))
);

CREATE INDEX company_contents_company_owner_idx
    ON company_contents (company_id, owner_id, updated_at DESC);
