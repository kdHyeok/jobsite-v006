-- 기업 정보 확장 + 채용공고 도메인.
-- 지원 진행 상태는 공고가 갖는다. 기업의 status 는 제거한다(같은 사실이 두 곳에 있으면 어긋난다).

-- ── 기업 확장 ───────────────────────────────────────────────────────────────
ALTER TABLE companies
    ADD COLUMN company_size   VARCHAR(20),
    ADD COLUMN annual_revenue BIGINT,
    ADD COLUMN employee_count INTEGER,
    ADD COLUMN address        VARCHAR(200),
    ADD COLUMN founded_on     DATE;

ALTER TABLE companies ADD CONSTRAINT companies_size_check
    CHECK (company_size IS NULL OR company_size IN ('STARTUP', 'SMALL', 'MEDIUM', 'LARGE', 'PUBLIC'));

ALTER TABLE companies ADD CONSTRAINT companies_employee_count_check
    CHECK (employee_count IS NULL OR employee_count >= 0);

ALTER TABLE companies ADD CONSTRAINT companies_annual_revenue_check
    CHECK (annual_revenue IS NULL OR annual_revenue >= 0);

-- 업종은 다중값이 되었다.
CREATE TABLE company_industries (
    company_id UUID        NOT NULL REFERENCES companies (id) ON DELETE CASCADE,
    industry   VARCHAR(60) NOT NULL,
    PRIMARY KEY (company_id, industry)
);

-- 기존 데이터 이전: 단일 industry -> 다중값 첫 항목, location -> address.
INSERT INTO company_industries (company_id, industry)
SELECT id, industry FROM companies
 WHERE industry IS NOT NULL AND btrim(industry) <> '';

UPDATE companies SET address = location
 WHERE location IS NOT NULL AND btrim(location) <> '';

-- 기존 status 는 버리지 않고 메모 끝에 한 줄로 남긴다. 사용자가 직접 정리하게 둔다.
UPDATE companies
   SET memo = btrim(coalesce(memo || E'\n\n', '') || '[이전 상태] ' || status)
 WHERE status IS NOT NULL;

ALTER TABLE companies
    DROP CONSTRAINT companies_status_check,
    DROP COLUMN status,
    DROP COLUMN industry,
    DROP COLUMN location;

-- ── 채용공고 ────────────────────────────────────────────────────────────────
CREATE TABLE job_postings (
    id                    UUID PRIMARY KEY,
    owner_id              UUID         NOT NULL REFERENCES app_users (id) ON DELETE CASCADE,
    -- 회사 없이도 공고를 만들 수 있다. 회사를 지우면 그 회사 공고도 함께 사라진다.
    company_id            UUID         REFERENCES companies (id) ON DELETE CASCADE,
    company_name_snapshot VARCHAR(120),
    position              VARCHAR(160) NOT NULL,
    posting_url           VARCHAR(500),
    employment_type       VARCHAR(20)  NOT NULL,
    -- NULL 이면 상시채용. 정렬에서 맨 뒤로 간다.
    deadline_at           TIMESTAMPTZ,
    stage                 VARCHAR(20)  NOT NULL,
    headcount             VARCHAR(60),
    work_location         VARCHAR(160),
    qualifications        TEXT,
    responsibilities      TEXT,
    required_skills       TEXT,
    -- NULL 이 아니면 보관함. 마감 지난 관심 공고를 조회 시점에 여기로 옮긴다.
    archived_at           TIMESTAMPTZ,
    created_at            TIMESTAMPTZ  NOT NULL,
    updated_at            TIMESTAMPTZ  NOT NULL,
    CONSTRAINT job_postings_employment_type_check
        CHECK (employment_type IN ('FULL_TIME', 'CONTRACT', 'INTERN', 'PART_TIME', 'DISPATCH')),
    CONSTRAINT job_postings_stage_check
        CHECK (stage IN ('INTERESTED', 'DRAFTING', 'SUBMITTED', 'CODING_TEST', 'INTERVIEW', 'AWAITING_RESULT'))
);

-- 목록은 항상 소유자로 좁힌 뒤 마감 임박 순으로 본다.
CREATE INDEX job_postings_owner_deadline_idx
    ON job_postings (owner_id, archived_at, deadline_at);

-- 기업 상세의 채용정보 카드가 회사별로 조회한다.
CREATE INDEX job_postings_company_idx
    ON job_postings (company_id) WHERE company_id IS NOT NULL;
