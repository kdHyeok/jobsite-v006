# nginx · Compose · 배포

## nginx (`frontend/nginx.conf`)

컨테이너 안에서 **80** 을 듣고 밖에서는 **8088**(로컬) 또는 **8443**(tailnet) 으로 보인다. 이 차이가 이 프로젝트 버그의 절반이었다.

규칙:
- `absolute_redirect off;` — 리다이렉트는 상대 경로. 켜면 `Location: http://127.0.0.1/` 처럼 포트가 빠진다.
- 프록시 헤더는 `$http_host`(브라우저가 보낸 Host 원본). `$host` 는 포트를 떼고 `$server_port` 는 80 이다.
- `X-Forwarded-Port` 를 보내지 않는다. 절대 URL 이 필요한 곳은 백엔드가 `PUBLIC_BASE_URL` 로 만든다.

백엔드로 넘기는 접두사(파일 상단 표와 동일): `/api/`, `/oauth2/`, `/login/`, `/swagger-ui`, `/v3/`.
`/admin` 은 `auth_request` → `/api/auth/admin-check`(204 면 index.html, 아니면 302 `/`). 데이터 보호는 백엔드가 하고 이 게이트는 UI 노출 방지용이다.

경로를 추가하면 `nginx.conf` 표 · `ApiPaths.java` · `routes.ts` 를 함께 갱신한다.

## Compose

- `compose.yaml` 하나로 로컬·서버 모두 소스에서 빌드한다. 이미지 레지스트리를 쓰지 않는다.
- 비밀값: `.secrets/postgres_password`, `.secrets/google_client_secret` → compose secret → 컨테이너 `/run/secrets/<프로퍼티키>` → Spring `configtree`. 파일 이름이 프로퍼티 키다.
- 설정: `.env` 의 `PUBLIC_BASE_URL`, `APP_ADMIN_EMAIL`, `GOOGLE_CLIENT_ID`, `SESSION_COOKIE_SECURE`(HTTPS 뒤에서만 true).
- 새 키를 추가하면 `.env.example`·`compose.yaml`·`docs/local-dev.md` 세 곳.

## 배포 — 서버에서 직접 pull

CI/CD 파이프라인은 없다. 서버에 SSH 로 들어가 `git pull` 하고 다시 올린다.

### 서버 준비 (1회)

```bash
git clone https://github.com/kdHyeok/jobsite-v006.git ~/jobsite-v006
cd ~/jobsite-v006
cp .env.example .env
mkdir -p .secrets && chmod 700 .secrets
python3 -c 'import secrets; print(secrets.token_urlsafe(36))' > .secrets/postgres_password
printf '%s' '<Google 클라이언트 시크릿>' > .secrets/google_client_secret
chmod 644 .secrets/*
```

`.env` 를 tailnet 값으로 채운다. `.env` 와 `.secrets/` 는 `.gitignore` 대상이라 `git pull` 이 덮어쓰지 않는다.

```
PUBLIC_BASE_URL=https://<머신>.<tailnet>.ts.net:8443
SESSION_COOKIE_SECURE=true
APP_ADMIN_EMAIL=<본인 Gmail>
GOOGLE_CLIENT_ID=<클라이언트 ID>
```

Google Cloud Console 의 Authorized redirect URI 에 `{PUBLIC_BASE_URL}/login/oauth2/code/google` 을 추가한다. 로컬용과 별개 항목이다.

### 매 배포

```bash
cd ~/jobsite-v006
git pull
docker compose up -d --build
bash scripts/smoke.sh          # PASS 여야 배포 완료
```

`smoke.sh` 는 `.env` 의 `PUBLIC_BASE_URL` 을 읽는다. 서버에서 tailnet 주소로 도는지 출력 첫 줄에서 확인한다.

### 롤백

```bash
git log --oneline -5
git checkout <이전 커밋>
docker compose up -d --build
```

마이그레이션이 이미 적용된 뒤라면 코드만 되돌려도 스키마는 남는다. Flyway 는 되돌리지 않으므로, 스키마를 바꾼 배포의 롤백은 되돌림 마이그레이션을 새로 쓴다.

## tailnet 노출

로컬 `127.0.0.1:8088` 에서 스모크가 PASS 한 뒤에만:

```bash
tailscale serve --bg --yes --https=8443 http://127.0.0.1:8088
```

`tailscale serve` 가 nginx 에 넘기는 Host 가 `127.0.0.1:8088` 일 수 있다. `redirect_uri` 는 `PUBLIC_BASE_URL` 로 만들어지므로 영향이 없지만, 스모크의 redirect_uri 줄로 실제 값을 반드시 본다.

Tailscale Serve 접근은 같은 tailnet 에 로그인하고 ACL 상 허용된 사용자로 제한된다. tailnet 안에서도 서비스 로그인은 별도로 필요하다.
