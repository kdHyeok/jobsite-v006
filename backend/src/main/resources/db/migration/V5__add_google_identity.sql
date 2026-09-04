-- Google 계정으로 가입한 사용자는 비밀번호가 없다.
ALTER TABLE app_users ALTER COLUMN password_hash DROP NOT NULL;

-- Google의 안정적인 식별자(sub). 이메일은 바뀔 수 있으므로 sub 를 기준으로 계정을 찾는다.
ALTER TABLE app_users ADD COLUMN google_sub VARCHAR(255);

-- NULL 은 여러 행이 허용되므로 Google 미연동 계정에는 영향이 없다.
CREATE UNIQUE INDEX app_users_google_sub_key ON app_users (google_sub);

-- 로그인 수단이 하나도 없는 계정은 만들 수 없게 한다.
ALTER TABLE app_users ADD CONSTRAINT app_users_credential_check
    CHECK (password_hash IS NOT NULL OR google_sub IS NOT NULL);
