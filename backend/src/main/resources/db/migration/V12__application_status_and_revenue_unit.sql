-- 지원 상태 세분화와 매출 입력 단위 보존 — docs/job-postings.md, docs/decisions.md.
ALTER TABLE job_postings DROP CONSTRAINT job_postings_status_check;
ALTER TABLE job_postings ALTER COLUMN status TYPE VARCHAR(30);
ALTER TABLE job_postings ADD CONSTRAINT job_postings_status_check CHECK (status IN (
    'INTERESTED', 'DRAFTING', 'SUBMITTED', 'WRITTEN_TEST_PREP', 'INTERVIEW_PREP', 'ACCEPTED',
    'DOCUMENT_REJECTED', 'WRITTEN_TEST_REJECTED', 'INTERVIEW_REJECTED', 'CLOSED'
));

ALTER TABLE companies ADD COLUMN revenue_unit VARCHAR(20);
UPDATE companies
   SET revenue_unit = CASE
       WHEN annual_revenue IS NULL THEN NULL
       WHEN annual_revenue >= 100000000 AND mod(annual_revenue, 100000000) = 0 THEN 'HUNDRED_MILLION'
       ELSE 'TEN_THOUSAND'
   END;
ALTER TABLE companies ADD CONSTRAINT companies_revenue_unit_check
    CHECK (revenue_unit IS NULL OR revenue_unit IN ('TEN_THOUSAND', 'HUNDRED_MILLION'));
