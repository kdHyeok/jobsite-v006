# JobSight v0.0.6

Google 계정으로 로그인하는 기업 정보 CRUD. 계정별로 데이터가 분리되고, 첫 로그인은 관리자 승인 후 이용한다(관리자가 자동 승인으로 바꿀 수 있다).

PostgreSQL 17 · Spring Boot 4.1 (Java 21) · Vue 3 (TypeScript) · nginx · Docker Compose.

## 시작

[docs/local-dev.md](docs/local-dev.md) 를 따른다. 요약:

```bash
cp .env.example .env            # APP_ADMIN_EMAIL 을 본인 Gmail 로
mkdir -p .secrets && chmod 700 .secrets
python3 -c 'import secrets; print(secrets.token_urlsafe(36))' > .secrets/postgres_password
touch .secrets/google_client_secret
docker compose up -d --build
bash scripts/smoke.sh           # PASS 확인
```

`http://127.0.0.1:8088`. Google 로그인은 Console 에 `http://127.0.0.1:8088/login/oauth2/code/google` 을 등록하고 `GOOGLE_CLIENT_ID`·`.secrets/google_client_secret` 을 채워야 활성화된다.

## 문서

| | |
|---|---|
| 작업 규칙·불변 조건·검증 절차 | [AGENTS.md](AGENTS.md) |
| 로컬 실행·환경 문제 | [docs/local-dev.md](docs/local-dev.md) |
| 백엔드 | [docs/backend.md](docs/backend.md) |
| 프런트엔드 | [docs/frontend.md](docs/frontend.md) |
| nginx · Compose · 배포 | [docs/nginx-deploy.md](docs/nginx-deploy.md) |
| 결정 기록 | [docs/decisions.md](docs/decisions.md) |
| API 목록 | 관리자 로그인 후 `/swagger-ui/index.html` |

## 범위 밖

AI, 공고 수집, 칸반, RLS, pgvector, 비밀번호 로그인, 이메일 발송.
