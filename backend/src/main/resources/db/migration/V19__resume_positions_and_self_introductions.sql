CREATE TABLE resume_positions (
    resume_id   UUID NOT NULL REFERENCES resumes (id) ON DELETE CASCADE,
    position_id UUID NOT NULL REFERENCES positions (id) ON DELETE CASCADE,
    PRIMARY KEY (resume_id, position_id)
);

CREATE INDEX resume_positions_position_idx ON resume_positions (position_id);

CREATE TABLE self_introductions (
    id         UUID PRIMARY KEY,
    owner_id   UUID          NOT NULL REFERENCES app_users (id) ON DELETE CASCADE,
    resume_id  UUID          NOT NULL REFERENCES resumes (id) ON DELETE CASCADE,
    question   VARCHAR(1000) NOT NULL,
    answer     TEXT,
    created_at TIMESTAMPTZ   NOT NULL,
    updated_at TIMESTAMPTZ   NOT NULL
);

CREATE INDEX self_introductions_owner_updated_idx ON self_introductions (owner_id, updated_at DESC);
CREATE INDEX self_introductions_resume_idx ON self_introductions (resume_id);
