-- 사용자가 스스로 바꿀 수 있는 표시 이름. 첫 Google 로그인 때 name 클레임으로 채운다.
-- 이메일은 Google 신원이자 관리자 규칙·계정 연결의 키이므로 편집 대상이 아니다.
ALTER TABLE app_users ADD COLUMN display_name VARCHAR(80);
