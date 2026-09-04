-- 토글의 의미가 바뀐다: "가입 허용/거부" → "자동 승인/승인 필요". 가입 거부 상태는 없어진다.
-- 이름을 그대로 두고 의미만 바꾸면 다음 사람이 오판하므로 컬럼을 새 의미로 개명한다.
ALTER TABLE app_settings RENAME COLUMN signup_enabled TO auto_approve_signup;

-- 지금까지 true 는 "가입 신청 접수 후 승인 필요" 였다. 동작을 보존하려면 새 의미에서는 false 다.
-- 자동 승인을 원하면 관리자 화면에서 켠다.
UPDATE app_settings SET auto_approve_signup = FALSE;
