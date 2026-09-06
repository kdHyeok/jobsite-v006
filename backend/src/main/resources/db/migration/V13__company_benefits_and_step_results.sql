-- 기업 복지와 채용 절차의 예정·완료 단순화 — docs/job-postings.md, docs/decisions.md.
ALTER TABLE companies ADD COLUMN benefits TEXT;

UPDATE recruitment_steps SET result = 'UPCOMING' WHERE result = 'IN_PROGRESS';
UPDATE recruitment_steps SET result = 'PASSED' WHERE result = 'FAILED';
ALTER TABLE recruitment_steps DROP CONSTRAINT recruitment_steps_result_check;
ALTER TABLE recruitment_steps ADD CONSTRAINT recruitment_steps_result_check
    CHECK (result IN ('UPCOMING', 'PASSED'));
