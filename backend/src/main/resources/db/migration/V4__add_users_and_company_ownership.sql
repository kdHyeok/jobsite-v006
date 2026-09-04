CREATE TABLE app_users (
    id UUID PRIMARY KEY,
    email VARCHAR(190) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT app_users_role_check CHECK (role IN ('USER', 'ADMIN')),
    CONSTRAINT app_users_status_check CHECK (status IN ('PENDING', 'ACTIVE', 'SUSPENDED', 'REJECTED'))
);

-- 이메일은 대소문자 구분 없이 유일하다.
CREATE UNIQUE INDEX app_users_email_lower_key ON app_users (LOWER(email));

CREATE TABLE app_settings (
    id SMALLINT PRIMARY KEY,
    signup_enabled BOOLEAN NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT app_settings_singleton CHECK (id = 1)
);

INSERT INTO app_settings (id, signup_enabled, updated_at)
VALUES (1, TRUE, CURRENT_TIMESTAMP);

-- 부트스트랩 관리자 자리.
-- 비밀번호는 기동 시 compose secret(app.bootstrap-admin.password)으로 설정된다.
-- 아래 placeholder는 BCrypt 해시 형식이 아니므로 어떤 입력과도 일치하지 않는다.
-- 즉 이 프로젝트에는 기본 비밀번호가 존재하지 않는다.
INSERT INTO app_users (id, email, password_hash, role, status, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-0000000000ad',
    'bootstrap-admin@invalid',
    '!disabled-until-configured!',
    'ADMIN',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- 기존 기업 데이터는 부트스트랩 관리자 소유로 이전한다.
ALTER TABLE companies ADD COLUMN owner_id UUID;

UPDATE companies SET owner_id = '00000000-0000-0000-0000-0000000000ad';

ALTER TABLE companies ALTER COLUMN owner_id SET NOT NULL;

ALTER TABLE companies ADD CONSTRAINT companies_owner_fk
    FOREIGN KEY (owner_id) REFERENCES app_users (id) ON DELETE CASCADE;

-- 목록 조회는 항상 소유자로 먼저 좁히므로 복합 인덱스로 교체한다.
DROP INDEX companies_updated_at_idx;

CREATE INDEX companies_owner_updated_at_idx ON companies (owner_id, updated_at DESC);
