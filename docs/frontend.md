# frontend — Vue 3 / TypeScript / Vite

## 진입점

| 무엇 | 어디 |
|---|---|
| 모든 경로 상수 | `src/routes.ts` — `ROUTES`(SPA), `API`(백엔드), `GOOGLE_LOGIN_URL`. 다른 곳에 경로 리터럴을 쓰지 않는다 |
| 셸·라우팅·세션 | `src/App.vue` — `pathname` 으로 `/`·`/postings`·`/admin` 분기. 라우터 라이브러리 없음 |
| HTTP + CSRF | `src/api/http.ts` — `XSRF-TOKEN` 쿠키를 `X-XSRF-TOKEN` 헤더로 자동 첨부 |
| 화면 | `LoginView.vue`(Google 버튼만) · `CompanyWorkspace.vue`(기업) · `PostingBoard.vue`(공고 D-day) · `AdminView.vue`(승인·권한·가입 토글) |
| 상세·수정 | `Drawer.vue` 하나가 보기/수정/추가를 모두 맡는다. 폼은 껍데기 없이 드로어 안에 들어간다 |
| 도메인 계산 | `src/types/posting.ts` 의 D-day·마감 시각 변환. 컴포넌트에 흩뿌리지 않는다 — `docs/job-postings.md` |
| 타입 | `src/types/*.ts` — 백엔드 DTO 와 1:1 |
| 스타일 | `src/styles/main.css` 한 파일. 토큰과 배치 규칙은 [docs/design-system.md](design-system.md) |

## 원칙

- **base URL 을 모른다.** nginx 뒤 same-origin 이라 상대 경로만 쓴다. 절대 URL 을 만들면 포트 차이에 노출된다.
- **로그인은 fetch 가 아니라 네비게이션.** `<a :href="GOOGLE_LOGIN_URL">`. 실패는 `/?authError=CODE` 로 돌아오며 `LoginView` 가 문구로 바꾸고 쿼리를 지운다.
- **상태 보유자는 화면당 하나.** 자식은 props-down / events-up. Pinia 없음.
- **목록은 핵심만, 나머지는 클릭.** 카드에 필드를 더하고 싶으면 드로어 상세에 넣는다 — design-system.md 규칙 3.
- **에러 계약**: 백엔드 `ApiError.fieldErrors` → `ApiClientError.fieldErrors` → 폼 필드 메시지.

## 테스트

`src/tests/*.spec.ts` (Vitest + @vue/test-utils). `api/*` 는 `vi.mock` 으로 대체한다. 새 API 함수를 추가하면 `App.spec.ts` 의 mock 목록에도 추가해야 한다.

```bash
npm run type-check && npm test
```

## 빌드

`npm run build` = `vue-tsc -b && vite build`. Dockerfile 이 이걸 실행하므로 타입 오류는 이미지 빌드 실패로 드러난다.
