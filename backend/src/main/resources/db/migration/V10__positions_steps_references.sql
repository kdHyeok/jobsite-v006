-- 채용공고를 "공고 / 모집 직무 / 절차 / 참고 정보" 로 나눈다 — docs/job-postings.md.
-- 데이터는 지우지 않는다. 스냅샷 기업은 진짜 기업으로, 절차성 stage 는 step 으로, 공고 텍스트는 직무 1개로 옮긴다.

-- ── 1. 기업 이름 유일성 (공백 제거 + 소문자) ────────────────────────────────
-- 이미 겹치는 이름이 있으면 뒤 행에 번호를 붙여 살려 둔다.
WITH ranked AS (
    SELECT id,
           row_number() OVER (PARTITION BY owner_id, lower(replace(name, ' ', '')) ORDER BY created_at, id) AS rn
      FROM companies
)
UPDATE companies c
   SET name = left(c.name, 115) || ' (' || r.rn || ')'
  FROM ranked r
 WHERE c.id = r.id AND r.rn > 1;

CREATE UNIQUE INDEX companies_owner_name_key
    ON companies (owner_id, lower(replace(name, ' ', '')));

-- ── 2. 스냅샷 기업 → 실제 기업 ─────────────────────────────────────────────
INSERT INTO companies (id, owner_id, name, created_at, updated_at)
SELECT gen_random_uuid(), s.owner_id, s.company_name_snapshot, now(), now()
  FROM (SELECT DISTINCT owner_id, company_name_snapshot
          FROM job_postings
         WHERE company_id IS NULL
           AND company_name_snapshot IS NOT NULL
           AND btrim(company_name_snapshot) <> '') s
 WHERE NOT EXISTS (
         SELECT 1 FROM companies c
          WHERE c.owner_id = s.owner_id
            AND lower(replace(c.name, ' ', '')) = lower(replace(s.company_name_snapshot, ' ', '')));

UPDATE job_postings p
   SET company_id = c.id
  FROM companies c
 WHERE p.company_id IS NULL
   AND p.company_name_snapshot IS NOT NULL
   AND c.owner_id = p.owner_id
   AND lower(replace(c.name, ' ', '')) = lower(replace(p.company_name_snapshot, ' ', ''));

-- 이름조차 없던 공고는 소유자별 "(회사 미입력)" 기업에 붙인다.
INSERT INTO companies (id, owner_id, name, created_at, updated_at)
SELECT gen_random_uuid(), owner_id, '(회사 미입력)', now(), now()
  FROM (SELECT DISTINCT owner_id FROM job_postings WHERE company_id IS NULL) s
 WHERE NOT EXISTS (SELECT 1 FROM companies c WHERE c.owner_id = s.owner_id AND c.name = '(회사 미입력)');

UPDATE job_postings p
   SET company_id = c.id
  FROM companies c
 WHERE p.company_id IS NULL AND c.owner_id = p.owner_id AND c.name = '(회사 미입력)';

ALTER TABLE job_postings
    ALTER COLUMN company_id SET NOT NULL,
    DROP COLUMN company_name_snapshot;

-- ── 3. 이름 정리 ────────────────────────────────────────────────────────────
ALTER TABLE job_postings RENAME COLUMN position TO title;
ALTER TABLE job_postings RENAME COLUMN stage TO status;

-- ── 4. 절차 (내 상태와 회사 절차를 분리) ────────────────────────────────────
CREATE TABLE recruitment_steps (
    id           UUID PRIMARY KEY,
    posting_id   UUID        NOT NULL REFERENCES job_postings (id) ON DELETE CASCADE,
    -- @OrderColumn 이 관리하는 0-based 순서.
    seq          INTEGER     NOT NULL,
    name         VARCHAR(60) NOT NULL,
    result       VARCHAR(20) NOT NULL,
    scheduled_at TIMESTAMPTZ,
    memo         TEXT,
    CONSTRAINT recruitment_steps_result_check
        CHECK (result IN ('UPCOMING', 'IN_PROGRESS', 'PASSED', 'FAILED')),
    UNIQUE (posting_id, seq)
);

-- 코테·면접·결과대기 는 절차 단계였다. 지원완료 + 진행 중인 step 하나로 옮긴다.
INSERT INTO recruitment_steps (id, posting_id, seq, name, result)
SELECT gen_random_uuid(), id, 0,
       CASE status WHEN 'CODING_TEST' THEN '코딩테스트'
                   WHEN 'INTERVIEW'   THEN '면접'
                   ELSE '결과 발표' END,
       'IN_PROGRESS'
  FROM job_postings
 WHERE status IN ('CODING_TEST', 'INTERVIEW', 'AWAITING_RESULT');

UPDATE job_postings SET status = 'SUBMITTED'
 WHERE status IN ('CODING_TEST', 'INTERVIEW', 'AWAITING_RESULT');

ALTER TABLE job_postings DROP CONSTRAINT job_postings_stage_check;
ALTER TABLE job_postings ADD CONSTRAINT job_postings_status_check
    CHECK (status IN ('INTERESTED', 'DRAFTING', 'SUBMITTED', 'CLOSED'));

-- ── 5. 모집 직무 ────────────────────────────────────────────────────────────
CREATE TABLE positions (
    id               UUID PRIMARY KEY,
    owner_id         UUID         NOT NULL REFERENCES app_users (id) ON DELETE CASCADE,
    posting_id       UUID         NOT NULL REFERENCES job_postings (id) ON DELETE CASCADE,
    name             VARCHAR(160) NOT NULL,
    team             VARCHAR(120),
    role             VARCHAR(200),
    responsibilities TEXT,
    impact           TEXT,
    growth           TEXT,
    experience       TEXT,
    required_skills  TEXT,
    preferred_skills TEXT,
    headcount        VARCHAR(60),
    work_location    VARCHAR(160),
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL
);
CREATE INDEX positions_owner_idx   ON positions (owner_id);
CREATE INDEX positions_posting_idx ON positions (posting_id);

CREATE TABLE position_tech_stack (
    position_id UUID        NOT NULL REFERENCES positions (id) ON DELETE CASCADE,
    tech        VARCHAR(60) NOT NULL,
    PRIMARY KEY (position_id, tech)
);

-- 기존 공고마다 제목 이름의 직무 하나. 직무 성격의 텍스트 필드는 여기로.
INSERT INTO positions (id, owner_id, posting_id, name, responsibilities, required_skills,
                       headcount, work_location, created_at, updated_at)
SELECT gen_random_uuid(), owner_id, id, title, responsibilities, required_skills,
       headcount, work_location, created_at, updated_at
  FROM job_postings;

ALTER TABLE job_postings
    DROP COLUMN responsibilities,
    DROP COLUMN required_skills,
    DROP COLUMN headcount,
    DROP COLUMN work_location,
    ADD COLUMN target_position_id UUID REFERENCES positions (id) ON DELETE SET NULL;

-- ── 6. 참고 정보 (계정 안에서 공유, 직무와 N:M) ─────────────────────────────
CREATE TABLE reference_items (
    id                  UUID PRIMARY KEY,
    owner_id            UUID         NOT NULL REFERENCES app_users (id) ON DELETE CASCADE,
    kind                VARCHAR(30)  NOT NULL,
    title               VARCHAR(200) NOT NULL,
    url                 VARCHAR(500),
    memo                TEXT,
    -- "참고 직무" 는 내부 직무를 가리킨다. 그 직무가 지워지면 카드는 제목으로 남는다.
    related_position_id UUID         REFERENCES positions (id) ON DELETE SET NULL,
    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL,
    CONSTRAINT reference_items_kind_check
        CHECK (kind IN ('RELATED_POSITION', 'EXPERIENCED_POSTING', 'SENIOR_INTERVIEW', 'ARTICLE', 'OTHER'))
);
CREATE INDEX reference_items_owner_idx ON reference_items (owner_id);

CREATE TABLE position_references (
    position_id  UUID NOT NULL REFERENCES positions (id) ON DELETE CASCADE,
    reference_id UUID NOT NULL REFERENCES reference_items (id) ON DELETE CASCADE,
    PRIMARY KEY (position_id, reference_id)
);
