-- 로그인은 Google 계정으로만 한다. 비밀번호 경로를 스키마에서 제거한다.
-- 행은 삭제하지 않는다. google_sub 가 없는 기존 행은 로그인 수단이 없는 상태로 남으며
-- 관리자 화면에서 "로그인 수단: 없음" 으로 표시되어 정리 여부를 사람이 결정한다.

ALTER TABLE app_users DROP CONSTRAINT app_users_credential_check;

ALTER TABLE app_users DROP COLUMN password_hash;
