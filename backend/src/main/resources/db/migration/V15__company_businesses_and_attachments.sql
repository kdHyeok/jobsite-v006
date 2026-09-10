-- 기업 주요 사업. 여러 건을 순서대로 기록한다(@OrderColumn 이 seq 를 채운다).
CREATE TABLE company_businesses (
    company_id  UUID         NOT NULL REFERENCES companies (id) ON DELETE CASCADE,
    seq         INTEGER      NOT NULL,
    name        VARCHAR(120) NOT NULL,
    description TEXT,
    PRIMARY KEY (company_id, seq)
);

-- 이력서 증빙 파일. 이력서 행은 여기 id 만 들고 있다 — docs/attachments.md.
CREATE TABLE attachments (
    id           UUID         PRIMARY KEY,
    owner_id     UUID         NOT NULL REFERENCES app_users (id) ON DELETE CASCADE,
    filename     VARCHAR(260) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes   INTEGER      NOT NULL,
    data         BYTEA        NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL
);

CREATE INDEX attachments_owner_idx ON attachments (owner_id, created_at DESC);
