# backend — Spring Boot 4.1 / Java 21

## 진입점

| 무엇 | 어디 |
|---|---|
| 모든 HTTP 경로 상수 | `common/ApiPaths.java` — 컨트롤러와 `SecurityConfig` 가 같은 상수를 쓴다 |
| 인가 규칙, CSRF, OAuth 실패 처리 | `auth/SecurityConfig.java` |
| Google 로그인 → 계정 해석 | `auth/GoogleOidcUserService.java` → `user/AppUserService.resolveGoogleUser` |
| 로그인 principal | `auth/AppOidcUser.java` (유일한 principal 타입) |
| 현재 사용자 읽기 | `auth/CurrentUser.java` — 서비스가 직접 호출, 컨트롤러는 소유자를 넘기지 않는다 |
| Google 클라이언트 등록 (조건부) | `config/GoogleOAuthConfig.java` |
| 에러 응답 형식 | `common/GlobalExceptionHandler.java`, `common/ApiError`, `common/ApiRuleException` |
| 스키마 | `resources/db/migration/V1~V13` — Flyway, `ddl-auto: validate` |
| 설정 | `resources/application.yml` — 비밀값은 `configtree:/run/secrets/` |

## 요청 한 번의 흐름

```
nginx → SecurityFilterChain
          ├ CsrfFilter (+CsrfCookieFilter: 토큰을 항상 쿠키로 내림)
          ├ OAuth2 필터: /oauth2/authorization/google, /login/oauth2/code/google
          └ AuthorizationFilter: ApiPaths 매처 → 401 / 403 JSON
        → Controller (@Valid) → Service (@Transactional, CurrentUser.id()) → Repository → PostgreSQL
```

## 계정 모델

- `app_users(id, email, google_sub, display_name, role, status)` — 식별자는 `google_sub`. 이메일은 검증된 경우에만 기존 계정 연결에 쓴다. `display_name` 만 사용자·관리자가 편집한다(첫 로그인 때 Google `name` 으로 초기화).
- 계정 삭제(`DELETE /api/admin/users/{id}`)는 `companies.owner_id` FK 의 `ON DELETE CASCADE` 로 소유 기업까지 지운다. 자기 자신·마지막 활성 관리자는 거부.
- 상태 `PENDING/ACTIVE/SUSPENDED/REJECTED`, 권한 `USER/ADMIN`. 로그인은 ACTIVE 만.
- 첫 Google 로그인의 초기 상태는 `app_settings.auto_approve_signup` 이 정한다: true → ACTIVE(바로 이용), false → PENDING(승인 대기). 가입을 막는 상태는 없다(V8).
- `APP_ADMIN_EMAIL` 계정은 `resolveGoogleUser` 가 매 로그인마다 ADMIN/ACTIVE 로 맞춘다. 새 배포에서는 V4 의 고정 행(`BOOTSTRAP_ADMIN_ID`)을 넘겨받아 시드 기업을 소유한다.
- `companies.owner_id` NOT NULL. 조회는 반드시 `…AndOwnerId`.

## 상태 검사는 트랜잭션 밖에서

`GoogleOidcUserService` 가 `resolveGoogleUser`(커밋됨) 뒤에 상태를 검사해 예외를 던진다. 안에서 던지면 방금 만든 PENDING 계정이 롤백되어 관리자 화면에 신청이 안 보인다.

## 버전 함정 (Spring Boot 4.1 / Spring Security 7.1)

- Jackson 3 → `import tools.jackson.databind.ObjectMapper`, 의존성 `spring-boot-starter-jackson`. `com.fasterxml.jackson.databind` 는 컴파일 클래스패스에 없다.
- CSRF → `CsrfTokenRequestAttributeHandler` 명시 (기본 `XorCsrfTokenRequestAttributeHandler` 는 쿠키 원본 토큰을 거부해 모든 POST 가 403).
- OAuth2 등록은 프로퍼티가 아니라 `GoogleOAuthConfig` 빈. `spring.security.oauth2.client.registration.*` 에 빈 client-id 가 있으면 기동 실패.
- springdoc 은 3.x (`springdoc-openapi-starter-webmvc-ui:3.1.0`). 2.x 는 Boot 4 에서 안 뜬다.
- `redirect_uri` 는 `app.public-base-url` 로 조립. `X-Forwarded-*` 추론 금지.

## 마이그레이션 규칙

적용된 `V*.sql` 은 수정하지 않는다. 새 번호로 추가한다. 데이터 삭제는 마이그레이션에 넣지 않고 사람이 결정한다(V6 참고).

기업별 뉴스·유튜브 자료는 `V11__company_contents.sql`과 `companycontent/*`가 담당한다. 회사·콘텐츠 조회 모두 `owner_id` 조건을 쿼리에 포함한다.

## 컴파일·테스트

컨테이너에서 `./gradlew compileJava compileTestJava`. 전체 `test` 는 Testcontainers 라 Docker 소켓이 필요하다(README). 종료코드는 `$?` 로 직접 읽는다.
