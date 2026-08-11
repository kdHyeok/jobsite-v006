# JobSight v0.0.6 기업 정보 CRUD

PostgreSQL, Spring Boot, Vue를 각각 Docker 컨테이너로 실행하는 단일 사용자 기업 정보 CRUD 데모입니다. 표시되는 초기 기업은 합성 데이터입니다.

## 범위

- 기업 목록·상세 조회
- 기업 생성·수정·삭제
- PostgreSQL 영속 저장
- Tailscale tailnet 전용 배포

인증, AI, 공고 수집, 칸반, RLS, pgvector는 이 버전에 포함하지 않습니다.

## 실행

```bash
cp .env.example .env
mkdir -p .secrets
chmod 700 .secrets
python3 -c 'import secrets; print(secrets.token_urlsafe(36))' > .secrets/postgres_password
chmod 644 .secrets/postgres_password
docker compose config --quiet
docker compose up -d --build
docker compose ps
```

DB 비밀번호는 Git에서 제외된 `.secrets/postgres_password`에 저장하고 Compose secret으로만 마운트합니다. `docker compose config --quiet`은 설정 유효성만 검사하며 비밀번호를 출력하지 않습니다.

로컬 주소: `http://127.0.0.1:8088`

```bash
curl -fsS http://127.0.0.1:8088/
curl -fsS http://127.0.0.1:8088/api/companies
```

## 테스트

frontend:

```bash
cd frontend
npm ci
npm run type-check
npm test
npm run build
cd ..
```

backend 전체 테스트는 Java 21과 실제 Docker PostgreSQL을 사용합니다. `v006/`에서 실행합니다.

```bash
docker run --rm --network host \
  -e TESTCONTAINERS_HOST_OVERRIDE=127.0.0.1 \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v jobsite-v006-gradle-cache:/root/.gradle \
  -v "$PWD/backend":/workspace -w /workspace \
  eclipse-temurin:21-jdk-alpine \
  ./gradlew test --no-daemon
```

실제 서비스는 `frontend`, `backend`, `db` 세 컨테이너입니다.

## 중지와 데이터

```bash
docker compose down
```

`docker compose down`은 named volume을 유지합니다. `docker compose down -v`는 PostgreSQL 데이터를 삭제하므로 사용 전에 별도 확인이 필요합니다.

## Tailscale 전환

새 stack을 `127.0.0.1:8088`에서 검증한 뒤에만:

```bash
tailscale serve --bg --yes --https=8443 http://127.0.0.1:8088
```

rollback:

```bash
systemctl --user enable --now job-site-demo.service
tailscale serve --bg --yes --https=8443 http://127.0.0.1:3015
```

Tailscale Serve 접근은 같은 tailnet에 로그인하고 ACL상 허용된 사용자로 제한됩니다.
