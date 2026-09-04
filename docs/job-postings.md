# 기업 정보 · 채용공고

## 목적

기업을 카드로 정리하고, 그 기업의 채용공고를 마감 임박 순으로 관리한다. 지원 진행 상태는 **공고**가 가진다(기업이 아니라).

## 도메인

```
app_users ──1:N──▶ companies ──1:N──▶ job_postings
                        │
                        └──1:N──▶ company_industries   (업종 다중값)

소유자(owner_id)는 companies·job_postings 양쪽에 있다.
공고를 회사 없이도 만들 수 있고(company_id NULL 허용),
회사를 지우면 그 회사 공고도 함께 지워진다(ON DELETE CASCADE).
```

### companies

| 필드 | 비고 |
|---|---|
| `name` | 필수, 120자 |
| `summary` | 간략 소개, TEXT |
| `website_url` | http/https |
| `industries` | **다중**. `company_industries` 테이블. IT서비스·금융·SI 등 자유 입력 |
| `company_size` | `STARTUP` `SMALL` `MEDIUM` `LARGE` `PUBLIC` |
| `annual_revenue` | 원 단위 `BIGINT`. 억/조 표기는 화면에서 포맷 |
| `employee_count` | `INT` |
| `address` | 200자 |
| `founded_on` | `DATE`. 연월만 입력받고 1일로 저장 |
| `memo` | 개인 메모, TEXT |

`status`(관심/지원준비/…)는 V9 에서 제거했다. 지원 진행은 공고가 근거다 — `docs/decisions.md` 참고.

### job_postings

| 필드 | 비고 |
|---|---|
| `company_id` | NULL 허용. 채워지면 기업 상세의 채용정보에 자동으로 뜬다 |
| `company_name_snapshot` | 회사 미연결 공고의 고용회사명 |
| `position` | 채용직무, 필수 |
| `posting_url` | 공고 링크 |
| `employment_type` | `FULL_TIME` `CONTRACT` `INTERN` `PART_TIME` `DISPATCH` |
| `deadline_at` | 서류마감 `TIMESTAMPTZ`. 상시채용이면 NULL |
| `stage` | `INTERESTED` `DRAFTING` `SUBMITTED` `CODING_TEST` `INTERVIEW` `AWAITING_RESULT` |
| `qualifications` `responsibilities` `required_skills` | TEXT |
| `headcount` | 모집인원 문자열(`0명`·`00명` 같은 표기 허용) |
| `work_location` | 근무지역 |
| `archived_at` | NULL 이 아니면 보관함 |

## 불변 조건

1. **소유자 격리**: `job_postings` 조회도 `…AndOwnerId`. 기업과 같은 규칙이다.
2. **공고-기업 소유자 일치**: 공고에 `company_id` 를 붙일 때 그 기업이 같은 소유자인지 검사한다. 아니면 404(존재를 알리지 않는다).
3. **D-day 정렬**: 마감 가까운 순. 마감 없는(상시) 공고는 맨 뒤. 보관된 공고는 목록에서 빠진다.
4. **자동 보관**: 마감이 지났는데 `stage=INTERESTED` 인 공고는 **목록 조회 시점에** `archived_at` 을 채운다. 스케줄러 없음 — `docs/decisions.md` 참고.
   자동 보관 대상은 `INTERESTED` 뿐이다. 지원한 공고(`SUBMITTED` 이후)는 마감이 지나도 결과를 기다리므로 그대로 둔다.

## 진입점

| 무엇 | 어디 |
|---|---|
| 스키마 | `V9__extend_company_and_add_job_postings.sql` |
| 공고 도메인 | `backend/.../posting/` — `JobPosting`, `JobPostingService`, `JobPostingController` |
| 자동 보관 | `JobPostingService.autoArchiveExpired()`. `findOpen()`/`findArchived()` 가 조회 트랜잭션 안에서 먼저 호출한다 |
| 기업 확장 | `backend/.../company/Company.java` — `industries` 는 `@ElementCollection`(`company_industries`) |
| 화면 | `CompanyWorkspace.vue`(기업) · `PostingBoard.vue`(공고 보드, 진행중/보관함 탭) · `PostingCard.vue`(양쪽 공용, `compact` 로 기업 상세용) · `PostingForm.vue` |
| D-day 계산·표기 | `frontend/src/types/posting.ts` — `daysUntil` `ddayLabel` `ddayTone` `formatDeadline` `toUtcIso` `toLocalInput` |
| 경로 상수 | `ApiPaths.POSTINGS`, `routes.ts` 의 `API.postings` |

## 실행·검증

```bash
docker compose up -d --build
bash scripts/smoke.sh                      # /api/postings 미인증 401 포함
cd frontend && npm run type-check && npm test   # D-day 계산은 posting.spec.ts 가 지킨다
```

공고 기능은 로그인 세션이 필요해 스모크로 끝까지 검증할 수 없다. 브라우저에서:
1. 기업 추가 → 업종 여러 개 입력 → 상세에 정보 카드가 뜨는가
2. 공고 추가(기업 연결) → 기업 상세 "채용정보"에 뜨는가
3. 마감을 과거로 두고 `관심` 저장 → 목록 새로고침 → 보관함으로 이동하는가
4. 마감 임박 순으로 정렬되는가, 상시채용이 맨 뒤인가

## 함정

- **마감 시각은 사용자의 로컬 시간대로 입력받고 UTC 로 저장한다.** `datetime-local` 값에는 시간대가 없어, 그대로 문자열로 보내면 서버가 UTC 로 오해한다. 프런트에서 `new Date(value).toISOString()` 으로 변환해 보낸다.
- **D-day 는 서버가 계산해 내려주지 않는다.** 응답의 `deadlineAt` 을 프런트가 사용자 시계로 계산한다. 서버가 계산하면 자정을 넘길 때 화면이 틀어진다.
- 자동 보관은 조회 때 일어나므로, 목록을 한 번도 안 열면 보관되지 않는다. 의도된 동작이다.
- **기업 목록 응답의 `openPostings` 는 항상 비어 있다**(N+1 회피). 채용정보를 보려면 `GET /api/companies/{id}` 상세를 따로 읽어야 한다 — `CompanyWorkspace` 가 선택 시 그렇게 한다.
- 정렬은 서버(`deadlineAt asc nulls last`)가 한다. 화면에서 다시 정렬하면 두 곳이 어긋난다.
