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

- `compose.yaml` 로컬 빌드. `compose.prod.yaml` 은 GHCR 이미지 override(서버용).
- 비밀값: `.secrets/postgres_password`, `.secrets/google_client_secret` → compose secret → 컨테이너 `/run/secrets/<프로퍼티키>` → Spring `configtree`. 파일 이름이 프로퍼티 키다.
- 설정: `.env` 의 `PUBLIC_BASE_URL`, `APP_ADMIN_EMAIL`, `GOOGLE_CLIENT_ID`, `SESSION_COOKIE_SECURE`(HTTPS 뒤에서만 true).
- 새 키를 추가하면 `.env.example`·`compose.yaml`·`docs/local-dev.md` 세 곳.

## 배포 방식 (현재)

**이미지 기반.** 서버에 git 은 필요 없다.

```
main push → GitHub Actions (.github/workflows/deploy.yml)
  build job: backend/frontend 이미지 → ghcr.io/kdhyeok/jobsite-v006-{backend,frontend}:{latest,sha}
  deploy job: tailscale/github-action 으로 tailnet 합류 → ssh 서버
              scp compose.yaml compose.prod.yaml → docker compose pull → up -d
```

서버 준비(1회): `~/jobsite-v006/` 에 `.env`(`PUBLIC_BASE_URL=https://<머신>.<tailnet>.ts.net:8443`, `SESSION_COOKIE_SECURE=true`) 와 `.secrets/` 두 파일, `docker login ghcr.io`(read:packages PAT). Google Console 에 tailnet redirect URI 도 등록.

GitHub secrets: `TS_OAUTH_CLIENT_ID`, `TS_OAUTH_SECRET`, `SSH_HOST`, `SSH_USER`, `SSH_PRIVATE_KEY`. Tailscale ACL 은 `tag:github-ci` → 서버 `:22` 만.

배포 후 서버에서: `bash scripts/smoke.sh https://<머신>.<tailnet>.ts.net:8443` 가 PASS 여야 한다.

롤백: `IMAGE_TAG=<이전sha> docker compose -f compose.yaml -f compose.prod.yaml up -d`.

## tailnet 전환 시 확인

`tailscale serve` 가 nginx 에 넘기는 Host 가 `127.0.0.1:8088` 일 수 있다. `redirect_uri` 는 `PUBLIC_BASE_URL` 로 만들어지므로 영향이 없지만, 스모크의 redirect_uri 줄로 실제 값을 반드시 본다.
