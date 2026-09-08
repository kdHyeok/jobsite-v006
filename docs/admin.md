# 관리자 콘솔

## 목적

계정을 승인·정지·삭제하고, 권한과 표시 이름을 바꾸고, 신규 가입 자동 승인 여부를 정한다.
계정마다 **등록한 데이터 개수**(기업·채용공고·모집 직무)를 함께 보여 어떤 계정이 실제로 쓰이는지 판단한다.

## 진입점

| 무엇 | 어디 |
|---|---|
| API | `admin/AdminController` — `ApiPaths.ADMIN`. 전체가 `SecurityConfig` 의 `hasRole('ADMIN')` 아래 |
| 계정 조작 | `user/AppUserService` — 승인·상태·권한·이름·삭제 |
| 가입 토글 | `setting/AppSettingService` — `auto_approve_signup` |
| **등록 데이터 개수** | `admin/AdminStatsService` + 세 리포지토리의 `countGroupedByOwner()` |
| 화면 | `frontend/src/components/AdminView.vue` (`/admin`) |
| 화면 진입 | 탑바 `☰ 메뉴` → `관리자`. ADMIN 에게만 보인다 |
| MCP | `admin_*` 도구 — `jobsight.admin` scope **와** 실제 `ROLE_ADMIN` 을 둘 다 요구 |

## 불변 조건

1. **관리자는 개수만 본다.** 소유자 격리(다른 계정 데이터는 404)의 유일한 예외이고, 그 예외는 **집계 숫자에 한정**된다.
   제목·회사명 같은 내용은 어떤 관리자 엔드포인트도 돌려주지 않는다 — `docs/decisions.md`.
2. **자기 계정과 마지막 활성 관리자는 못 건드린다.** 상태·권한 변경, 삭제 모두 400. 락아웃 방지.
3. **`APP_ADMIN_EMAIL` 이 이긴다.** 그 이메일은 로그인마다 ADMIN/ACTIVE 로 복구된다. 화면에서 내려도 소용없다.
4. **이메일은 편집 대상이 아니다.** Google 신원이자 계정 연결 키다. 관리자도 `display_name` 만 바꾼다.
5. **계정 삭제는 소유 데이터를 함께 지운다** (`owner_id` FK `ON DELETE CASCADE`). 확인 대화상자에 명시한다.

## 등록 데이터 개수

```
GET /api/admin/users/counts
→ [ { userId, companies, postings, positions } ]
```

- **쿼리 3개 고정.** 계정 수와 무관하다. 테이블마다 `group by owner_id` 로 한 번씩 센다 —
  계정마다 세면 계정 N 명에 3N 쿼리가 된다.
- **데이터가 하나도 없는 계정은 응답에서 빠진다.** 화면이 `0` 으로 채운다. 서버가 빈 행을 만들지 않는다.
- 계정 목록(`GET /users`)과 **분리된 엔드포인트**다. 상태·권한을 바꾸면 그 행만 새 `UserResponse` 로 갈아끼우는데,
  개수를 같은 DTO 에 넣었으면 그때마다 사라진다. 화면도 `dataCounts` 를 `userId` 로 따로 들고 있다.

## 실행·검증

```bash
bash scripts/smoke.sh                        # /api/admin/users/counts 미인증 401 포함
cd frontend && npm run type-check && npm test
```

관리자로 로그인한 뒤 브라우저에서:
1. `/admin` 표에 `기업 · 공고 · 직무` 세 열이 보이는가
2. 다른 계정 행에도 그 계정이 등록한 개수가 뜨는가 (내 계정 것만이 아니라)
3. 데이터가 없는 계정이 `0` 으로 보이는가 (빈칸이 아니라)
4. 어떤 계정의 상태를 바꾼 뒤에도 그 행의 개수가 그대로인가
5. 개수 조회가 실패해도 계정 목록은 그대로 뜨는가

## 함정

- **개수 API 를 계정 목록에 합치지 않는다.** 위 "등록 데이터 개수" 세 번째 항목 참고.
  합치면 `PATCH /users/{id}/status` 응답에 개수가 없어 행을 갈아끼울 때 사라진다.
- `GET /users/counts` 와 `GET /users/{id}` 는 충돌하지 않는다 — 후자가 아예 없다. 나중에 추가한다면 순서를 확인한다.
- 개수 조회 실패는 화면을 막지 않는다. `AdminView.load()` 가 `.catch(() => [])` 로 삼키고 `0` 을 보여준다.
  계정 관리는 개수보다 중요하다.
