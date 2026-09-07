-- 이력서. 버전마다 한 행, 섹션들은 JSONB 문서 하나 — docs/resumes.md.
-- 이름은 유일하지 않다. 같은 이름의 여러 버전을 id 로 구분한다.
CREATE TABLE resumes (
    id         UUID PRIMARY KEY,
    owner_id   UUID         NOT NULL REFERENCES app_users (id) ON DELETE CASCADE,
    name       VARCHAR(120) NOT NULL,
    content    JSONB        NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL
);

-- 목록은 항상 소유자로 좁혀 최근 수정 순으로 본다.
CREATE INDEX resumes_owner_updated_idx ON resumes (owner_id, updated_at DESC);
