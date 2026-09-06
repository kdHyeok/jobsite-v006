# JobSight MCP / OAuth 플러그인

## 목적과 진입점

`plugins/jobsight`를 ChatGPT/Codex용 플러그인으로 패키징한다. MCP는 기존 Spring 애플리케이션의
`/mcp`에서 JSON 응답형 Streamable HTTP로 실행하고, 기존 CRUD 컨트롤러·서비스와 DTO 검증을 재사용한다.
도구 설명은 기존 `@Operation`, 입력 스키마는 실제 요청 DTO에서 만든다. `SKILL.md`는 사이트 조작과 데이터 관계를 설명한다.

## 인증과 불변 조건

- 사용자 신원은 기존 Google OIDC만 사용한다. MCP OAuth는 그 사용자가 외부 클라이언트에 주는 위임이다.
- Spring Authorization Server의 authorization code + S256 PKCE + 동의 화면을 사용한다.
- 사전 등록 클라이언트만 사용하고 redirect URI를 정확히 비교한다. DCR·임의 callback 등록은 제공하지 않는다.
- `/mcp`는 세션 쿠키를 받지 않는 Bearer 전용 체인이다. 기존 REST/Google 세션의 CSRF는 유지한다.
- 매 MCP 요청에서 계정 활성 상태·현재 역할을 DB에서 다시 확인한다. 도구는 `jobsight.read`,
  `jobsight.write`, 관리자 작업은 추가로 `jobsight.admin` 및 실제 ADMIN 역할을 요구한다.
- resource는 `PUBLIC_BASE_URL + /mcp` 하나다. OAuth 토큰은 이 서버에서만 유효하며 Google 토큰을 전달하지 않는다.
- 초기 구현의 OAuth 승인·토큰은 단일 프로세스 메모리에 보관한다. 재시작 시 재연결이 필요하다.
  다중 replica 또는 재시작 후 연결 유지가 필요하면 Spring의 JDBC authorization/consent 저장소로 교체한다.
- 기업·공고 삭제 cascade, 마지막 직무 삭제 방지, 참고 정보 공유, 소유자 404 규칙은 기존 서비스가 담당한다.

## 실행·검증

원격 main의 공개 도메인 배포 설정과 MCP 변경을 함께 유지한다. 배포 검증은 HTTP 200만 보지 않고
OAuth 메타데이터의 issuer, 인증/토큰 URL, S256 선언과 MCP resource 값까지 확인한다.
사이트 HTML을 반환하는 이전 서버는 통과할 수 없어야 한다.

`./gradlew compileJava compileTestJava`, MCP 인증/도구 테스트,
프런트 타입·테스트, `scripts/smoke.sh`를 실행한다. 실제 Google 왕복과 ChatGPT 등록 여부는 별도로 기록한다.

## 함정

로컬 loopback·tailnet 전용 주소는 ChatGPT 클라우드에서 바로 접근할 수 없다. 공개 HTTPS 배포 또는
클라이언트가 지원하는 사설 네트워크 연결이 필요하다. 등록 가능한 패키지 작성과 공개 배포/스토어 승인은 별개다.
공고 수정의 positions/steps, 직무 참고 연결은 전체 교체다. 변경 전에 조회하고 유지할 값을 포함해야 한다.

공식 근거: https://developers.openai.com/plugins/build/auth
및 https://docs.spring.io/spring-security/reference/servlet/oauth2/authorization-server/configuration-model.html

## 등록 방법

1. 서버 `PUBLIC_BASE_URL`을 실제 HTTPS origin으로 설정하고 Google Console callback도 같은 origin으로 맞춘다.
2. ChatGPT의 개발자용 앱/플러그인 연결에서 MCP URL을 `{PUBLIC_BASE_URL}/mcp`, 인증을 OAuth로 설정한다.
   사전 등록 client ID는 `jobsight-plugin`, 인증 방식은 public client(`none`), client secret은 없다.
   authorization code + PKCE(S256)를 사용한다. DCR/CIMD는 켜지 않는다.
3. 그 화면에 표시된 **정확한 callback URL**을 아래 `MCP_REDIRECT_URIS`로 설정해 재기동한다.
   기본값은 `https://chatgpt.com/connector_platform_oauth_redirect`지만 새 연결의 callback ID URL과 다를 수 있다.
   여러 클라이언트는 쉼표로 정확한 URI를 나열한다. 와일드카드와 동적 localhost 포트는 허용하지 않는다.

```powershell
$env:MCP_REDIRECT_URIS = 'https://chatgpt.com/connector/oauth/<등록 화면의 callback ID>'
docker compose -f compose.yaml -f compose.mcp.yaml up -d --build
```

4. `jobsight.read jobsight.write`를 요청하고 Google 로그인 및 JobSight 동의 화면을 완료한다.
   관리자 기능이 필요하면 `jobsight.admin`도 요청한다. scope만으로 관리자 권한이 생기지 않는다.
5. `account_get`, 기업 목록부터 확인한다. 쿠키/토큰을 사람이 복사하는 방식으로 연결하지 않는다.
   토큰은 1시간 유효하며 만료·서버 재시작 후 OAuth로 다시 연결한다. refresh_token은 발급하지 않는다.
6. 스킬 포함 플러그인 업로드가 가능한 클라이언트에는 아래 ZIP을 등록한다. MCP만 연결한 경우
   스킬은 자동 설치되지 않으므로 `plugins/jobsight/skills/jobsight/SKILL.md`도 해당 클라이언트의 스킬 기능으로 등록한다.

```powershell
python plugins/jobsight/package_plugin.py --base-url https://실제-서비스-도메인 --output artifacts/jobsight-plugin.zip
```

배포 URL을 모르는 소스 `.mcp.json`은 로컬 개발 주소다. 패키저가 ZIP 안의 URL만 실제 값으로 바꾼다.
ZIP 루트는 `.codex-plugin/plugin.json`, `.mcp.json`, `skills/jobsight/SKILL.md`다.
로컬 Codex는 이 폴더를 개발 플러그인으로 설치하거나 HTTP MCP 서버로 연결한다. 사전 등록 client ID와
클라이언트가 사용하는 고정 callback을 설정할 수 있어야 한다. 지원하지 않는 클라이언트에 DCR 지원을 주장하지 않는다.
스토어 공개는 별도 심사·실제 공개 URL·개인정보처리방침 등 게시 요구사항을 충족한 후 진행한다.

## 도구 계약과 검증 범위

도구는 38개이며 입력은 `id`, `companyId`, `postingId`, `q`, `archived`, `seq`, `request`처럼 실제 메서드 인자를 사용한다.
도구명·동작별 사용법은 스킬에 있다. `/mcp`의 `tools/list`는 연결 계정의 scope/역할에 따라 목록을 줄인다.
관리자 도구 7개는 일반 사용자에게 노출하지 않으며 직접 이름을 호출해도 거부한다.
`posting_list`는 자동 보관을 일으켜 쓰기 scope와 readOnlyHint=false를 사용한다.
뉴스/유튜브·참고 상세 조회는 소유자 필터된 목록에서 ID를 찾는다.
MCP용 `position_create`는 기존 `PositionService` 트랜잭션에서 공고 소유권을 검증하고 직무를 추가한다.

인증 테스트는 외부 Google 자격증명 없이 인증된 세션을 주입하고 실제 Spring OAuth 필터의 코드 발급/교환을 검증한다.
실제 Google 왕복 및 ChatGPT 서비스 연결과 같다고 간주하지 않는다.

### 2026-09-07 검증 결과

- 컨테이너에서 `compileJava compileTestJava` 및 MCP/서비스/컨트롤러 테스트 27개: 종료코드 0, `BUILD SUCCESSFUL`.
- 프런트 `npm run type-check`, `npm test`: 종료코드 0, 테스트 40개 통과.
- 재빌드한 Compose 컨테이너 3개 healthy, `bash scripts/smoke.sh`: 종료코드 0, 25개 검사 PASS.
- 플러그인·스킬 공식 구조 검사 통과. 로컬 ZIP은 `artifacts/jobsight-plugin-local.zip`에 생성했다.
- 실제 Google 로그인 왕복은 내장 브라우저의 로컬 연결 오류로 검증하지 못했다.
  ChatGPT 연결은 공개 HTTPS 주소와 정확한 callback 설정 전이므로 검증하지 못했다.

### 원격 main 통합 검증

- `4586ba9`의 공개 도메인 배포 설정과 기존 UI 수정을 보존하면서 MCP 변경을 통합했다.
- 컨테이너의 `compileJava compileTestJava test --tests '*Mcp*Test' --tests '*ServiceTest' --tests '*ControllerTest' --rerun-tasks`: 종료코드 0, `BUILD SUCCESSFUL`.
- 프런트 타입 검사·40개 테스트 및 플러그인/스킬 구조 검사 통과.
- 로컬 Compose 재빌드 후 3개 컨테이너 healthy, 스모크 30개 PASS. OAuth 메타데이터의 S256·issuer·endpoint·resource를 확인했다.
- 이번 작업은 저장소 통합·커밋·push이며 원격 서버 재배포는 포함하지 않는다. 실제 Google 로그인 왕복과 ChatGPT 연결은 여전히 미검증이다.
- 서버 배포 시 ChatGPT가 표시한 callback을 `MCP_REDIRECT_URIS`로 설정하고 `compose.mcp.yaml`을 함께 적용해야 한다. 기본 compose만 재시작하면 사용자별 callback 설정이 반영되지 않는다.
