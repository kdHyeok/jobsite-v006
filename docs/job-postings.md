# 기업 · 채용공고 · 모집 직무 · 참고 정보

## 목적

채용공고를 마감 임박 순으로 관리하고, 공고 안의 **모집 직무**를 따로 들여다보며, 직무마다 **참고 정보**를 붙여 둔다.
내 참여 상태와 회사의 채용 절차는 서로 다른 축이라 따로 기록한다.

## 도메인

```
app_users
 └─1:N─ companies                    unique(owner_id, lower(replace(name,' ','')))
         └─1:N─ job_postings         company_id NOT NULL
                 │  title(모집 부문) · posting_url · employment_type · deadline_at
                 │  status(내 상태) · qualifications · target_position_id? · archived_at
                 ├─1:N─ recruitment_steps   seq · name · result · scheduled_at · memo
                 └─1:N─ positions (모집 직무)  ≥ 1 보장
                         │  name · team · role · responsibilities · impact · growth
                         │  experience · required_skills · preferred_skills · headcount · work_location
                         ├─ position_tech_stack   (ElementCollection)
                         └─N:M─ reference_items  via position_references
                                  kind · title · url · memo · related_position_id?
```

소유자(`owner_id`)는 `companies` `job_postings` `positions` `reference_items` 네 테이블 모두에 있다. 조인 없이 소유자 격리 쿼리를 쓰기 위해서다.

### 두 축 — 내 상태 vs 회사 절차

| 축 | 어디 | 값 |
|---|---|---|
| **내 참여 상태** `status` | `job_postings` | `INTERESTED` `DRAFTING` `SUBMITTED` `CLOSED` |
| **회사 절차 진행** | `recruitment_steps.result` | `UPCOMING` `IN_PROGRESS` `PASSED` `FAILED` |

이전 `stage` 의 `CODING_TEST` `INTERVIEW` `AWAITING_RESULT` 는 절차 단계다. V10 이 `SUBMITTED` + 그 이름의 step 하나로 옮겼다.
"현재 단계" 는 첫 `UPCOMING`/`IN_PROGRESS` step. 화면은 `[서류] → [인적성] → [면접]` 체인으로 그린다.

### 필드 위치

| 필드 | 어디 | 이유 |
|---|---|---|
| 모집 부문(제목), 링크, 고용형태, 마감, 지원자격 | 공고 | 공고 하나에 하나 |
| 담당업무, 요구/우대 역량, 기술 스택, 인원, 근무지, 팀, 역할, 영향력, 성장, 경험 | 직무 | 공채는 직무마다 다르다 |
| 절차 | 공고 | 결정 2 — 직무별 차이는 단계 이름에 적는다("코딩테스트(개발직군)") |

## 불변 조건

1. **소유자 격리**: 네 테이블 모두 `…AndOwnerId` 조회만. 타인 데이터는 404.
2. **공고는 직무가 1개 이상**: 직무 없이 저장하면 `title` 과 같은 이름의 직무를 하나 만든다. 마지막 직무는 지울 수 없다(409 `LAST_POSITION`).
3. **기업은 직접 입력하면 자동 생성**: `companyId` 없이 `companyName` 만 오면 공백 제거·소문자 기준으로 기존 기업을 찾고, 없으면 이름만 채운 기업을 만든다. `company_name_snapshot` 은 없다.
4. **D-day 정렬**: 마감 가까운 순, 상시(NULL)는 맨 뒤. 서버가 정렬한다.
5. **자동 보관**: 마감 지남 + `status ∈ {INTERESTED, DRAFTING}` 인 공고를 **목록 조회 시점에** 보관. `SUBMITTED` 로 옮기면 보관함에서 자동으로 꺼낸다.
6. **참고 정보는 계정 안에서 공유**(결정 1): 한 참고 정보가 여러 직무에 붙는다. 다른 사용자와는 공유하지 않는다.
7. **절차는 공고 단위**(결정 2). `seq` 는 `@OrderColumn` 이 관리하는 0-based 정수. 분기 없음.

## 진입점

| 무엇 | 어디 |
|---|---|
| 스키마 | `V9__…job_postings.sql` → **`V10__positions_steps_references.sql`** |
| 공고 | `posting/JobPosting` `JobPostingService` `JobPostingController` — `ApiPaths.POSTINGS` |
| 절차 | `posting/RecruitmentStep` (공고에 임베드, cascade) · `PATCH /postings/{id}/steps/{seq}` |
| 직무 | `position/Position` `PositionService` `PositionController` — `ApiPaths.POSITIONS` |
| 참고 정보 | `reference/ReferenceItem` `ReferenceService` `ReferenceController` — `ApiPaths.REFERENCES` · 연결은 `PUT /positions/{id}/references` |
| 기업 자동 생성 | `JobPostingService.resolveCompany()` + `CompanyRepository.findByOwnerIdAndNameKey()` |
| 화면 | `PostingBoard`(홈 `/`) · `PositionBoard`(`/positions`) · `CompanyWorkspace`(`/companies`) |
| D-day·절차 표기 | `frontend/src/types/posting.ts` — `daysUntil` `ddayLabel` `ddayTone` `formatDeadline` `nextStepResult` |
| 화면 간 이동 | `routes.ts` 의 `FOCUS_QUERY`. `App.navigate(route, id)` → `?focus=<id>` → 보드가 `applyFocus()`/`openById()` 로 그 드로어를 연다 |

## API 요약 (상세는 Swagger)

```
GET/POST        /api/postings              ?archived=
GET/PUT/DELETE  /api/postings/{id}         PUT 은 positions[]·steps[] 를 통째로 받는다
PATCH           /api/postings/{id}/status  {status}
PATCH           /api/postings/{id}/steps/{seq}  {result}
PATCH           /api/postings/{id}/archive ?archived=
GET             /api/positions             ?q=  (이름·팀·회사·스택 부분 일치)
GET/PUT/DELETE  /api/positions/{id}
PUT             /api/positions/{id}/references  {referenceIds[]}  집합 교체
GET/POST        /api/references            ?q=
PUT/DELETE      /api/references/{id}
```

## 실행·검증

```bash
docker compose up -d --build && docker compose logs backend | grep "now at version"   # v10
bash scripts/smoke.sh                       # /api/positions /api/references 미인증 401 포함
cd frontend && npm run type-check && npm test
```

로그인 뒤 브라우저에서:
1. 공고 추가 → 기업을 직접 입력 → 기업 탭에 생겼는가
2. 직무를 비우고 저장 → 직무 하나가 제목 이름으로 생겼는가
3. 절차 3개 입력 → 상세에서 체인이 보이고 노드를 누르면 결과가 바뀌는가
4. 마감 과거 + `작성중` → 목록 새로고침 → 보관함으로 갔는가
5. 직무 페이지에서 검색 → 드로어 → 참고 정보 추가(기존 검색 / 새로 만들기)
6. 기업 상세의 채용정보 행을 더블클릭 → 채용공고 화면에서 그 공고가 열리는가(주소창에 `?focus=`)
7. 공고 상세의 모집 직무 행을 더블클릭 → 모집 직무 화면에서 그 직무가 열리는가

## 함정

- **마감 시각은 로컬 입력 → UTC 저장.** `datetime-local` 값에는 시간대가 없다. `toUtcIso()` 로 변환해 보낸다.
- **D-day 는 서버가 내려주지 않는다.** 프런트가 사용자 시계로 계산한다.
- **기업 목록 응답의 `openPostings` 는 비어 있다**(N+1 회피). 상세에서만 채워진다.
- **공고 PUT 은 직무 목록을 통째로 받는다.** `id` 가 있는 항목은 갱신, 없는 항목은 생성, 요청에 빠진 기존 직무는 삭제된다 — 직무 상세(팀·역량 등)는 직무 페이지에서 편집하므로 공고 폼은 `id`+`name` 만 다룬다.
- 절차 `seq` 는 재정렬하면 바뀐다. step 을 밖에서 id 로 참조하지 않는다.
- **`?focus=` 는 이미 불러온 목록에서만 찾는다**(추가 조회 없음). 기업 상세의 채용정보는 마감 전·미보관 공고라 진행 중 목록에 반드시 있고, 직무는 전체 목록을 받으므로 항상 찾힌다. 보관된 공고를 focus 로 넘기면 아무 일도 일어나지 않는다.
- `reference_items.related_position_id` 는 직무를 지우면 NULL 이 된다(ON DELETE SET NULL). 카드는 제목으로 남는다.
