CREATE TABLE self_introduction_resumes (
    self_introduction_id UUID NOT NULL REFERENCES self_introductions(id) ON DELETE CASCADE,
    resume_id UUID NOT NULL REFERENCES resumes(id) ON DELETE CASCADE,
    PRIMARY KEY (self_introduction_id, resume_id)
);

INSERT INTO self_introduction_resumes (self_introduction_id, resume_id)
SELECT id, resume_id FROM self_introductions;

CREATE INDEX idx_self_introduction_resumes_resume
    ON self_introduction_resumes(resume_id, self_introduction_id);

ALTER TABLE self_introductions DROP COLUMN resume_id;
