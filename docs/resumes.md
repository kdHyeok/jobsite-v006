# 이력서

## 목적

이력서를 **여러 버전**으로 두고(이름 + id 로 구분), 버전마다 섹션 행을 자유롭게 추가·삭제·순서 변경한다.
섹션 구성은 사용자가 쓰던 이력서 양식을 그대로 옮겼다 — 기본정보 · 학력 · 교육이수 · 대내외활동 · 경력 · 수상 · 자격증 · SW 역량 · 프로젝트.

## 도메인

```
app_users ──1:N──▶ resumes
                     id · owner_id · name(이력서 명) · content JSONB · created_at · updated_at

content = {
  basic:        { name, phone, birthDate, email, address, portfolioUrl, githubUrl }
  educations:   [ { startYm, endYm, school, major, gpa } ]
  trainings:    [ { name, institution, startYm, endYm, description } ]
  activities:   [ { name, organizer, startYm, endYm, description } ]
  experiences:  [ { company, startYm, endYm, description } ]
  awards:       [ { name, issuer, awardedYm } ]
  certificates: [ { name, issuer, acquiredYm } ]
  skills:       [ { name, level, description } ]
  projects:     [ { name, headcount, startYm, endYm, summary, techStack, role, outcome, description, url } ]
}
```

**섹션마다 테이블을 만들지 않았다.** 이력서는 통째로 읽고 통째로 저장하는 문서이고, 자격증만 SQL 로 뽑을 일이 없다. 9개 테이블은 엔티티·DTO·리포지토리 40개를 뜻한다. 대신 `ResumeContent` Java record 가 모양과 검증(길이 제한)을 들고, 프런트 `types/resume.ts` 의 `SECTIONS` 가 같은 키로 화면을 그린다 — `docs/decisions.md`.

연월(`startYm` 등)은 **문자열**이다. 사용자는 `2024.03`, `202403`, `현재 교육 중` 처럼 쓴다. 날짜로 파싱하면 그 표현을 잃는다.

## 불변 조건

1. **소유자 격리**: `findByIdAndOwnerId` 만. 타인 이력서는 404.
2. **이름은 유일하지 않다.** 버전은 id 로 구분한다. 같은 이름의 v1·v2 가 있어도 된다 — 사용자 요구.
3. **REST 저장은 문서 통째 PUT.** REST 에 행 단위 API 는 없다. 화면은 행 추가·삭제·이동을 로컬에서 하고 `저장` 을 눌러야 DB 에 간다(저장 안 한 변경은 `변경됨` 표시 + 이탈 경고). **MCP 만** 행 도구를 갖는다 — 서버가 조회→수정→저장을 한 트랜잭션으로 묶는 편의 계층이지, 두 번째 저장 경로가 아니다.
4. **빈 값은 항상 같은 모양.** `content` 가 없으면 `ResumeContent.empty()`, 섹션이 `null` 이면 `[]` 로 정규화해 저장한다. 읽는 쪽이 `null` 검사를 안 하게.
5. **복제는 새 id.** `POST /resumes/{id}/copy` 는 content 를 그대로 든 새 행을 만든다. 원본은 건드리지 않는다.

## 진입점

| 무엇 | 어디 |
|---|---|
| 스키마 | `V14__resumes.sql` |
| 도메인 | `resume/Resume`(엔티티, `content` 는 `@JdbcTypeCode(SqlTypes.JSON)` String) · `ResumeContent`(record + 검증) · `ResumeService` · `ResumeController` — `ApiPaths.RESUMES` |
| JSON 변환 | `ResumeService.write()/read()` — Boot 4 의 Jackson 3 `tools.jackson.databind.ObjectMapper` |
| 화면 | `ResumeBoard.vue`(`/resumes` 목록 카드, `?focus=<id>` 면 편집기) · `ResumeEditor.vue`(전체 페이지 편집기) · `ResumeSection.vue`(섹션 하나 = 행 카드 + ↑↓× + 행 추가) |
| 섹션 정의 | `frontend/src/types/resume.ts` 의 `SECTIONS` — 라벨·필드·입력 종류. 섹션을 더하려면 **여기와 `ResumeContent`, `ResumeSection` enum 세 곳** |
| MCP 도구 | `McpTools` 의 `resume_*` 10개. 행 도구는 `McpActions` → `ResumeService.addRow/updateRow/deleteRow/updateBasic`. 입력 `ResumeRowRequest`(합집합 record) + `ResumeSection`(enum) — `docs/mcp-plugin.md` |

## API 요약 (상세는 Swagger)

```
GET    /api/resumes            → [ {id, name, updatedAt} ]      목록엔 content 없음
POST   /api/resumes            {name, content?}                  content 없으면 빈 문서
GET    /api/resumes/{id}       → {id, name, content, createdAt, updatedAt}
PUT    /api/resumes/{id}       {name, content}                   통째 교체
POST   /api/resumes/{id}/copy  {name}                            새 버전
DELETE /api/resumes/{id}

MCP (REST 에는 없음)
resume_list · resume_get · resume_create · resume_update · resume_copy · resume_delete
resume_basic_update {id, request:BasicInfo}
resume_row_add      {id, section, request}          section ∈ educations…projects, request 는 그 섹션 필드만
resume_row_update   {id, section, index, request}   index 0부터, 행 전체 교체
resume_row_delete   {id, section, index}
```

## 실행·검증

```bash
docker compose up -d --build && docker compose logs backend | grep "now at version"   # v14
bash scripts/smoke.sh                       # /api/resumes 미인증 401 포함
cd frontend && npm run type-check && npm test
```

로그인 뒤 브라우저에서:
1. 이력서 탭 → `이력서 추가` → 편집기가 열리고 이름이 `새 이력서` 인가
2. 학력 `행 추가` 3개 → ↑↓ 로 순서 바꾸고 × 로 하나 지운 뒤 `저장` → 새로고침해도 그대로인가
3. 이름을 바꾸고 저장 안 한 채 `← 목록` → 이탈 확인이 뜨는가
4. `복제` → 같은 내용의 새 카드가 생기고 원본은 그대로인가
5. 목록 카드에 이름·최근 수정만 보이는가

## 함정

- **`content` 컬럼은 `jsonb`, 엔티티 필드는 `String`.** Hibernate 7 의 JSON 매핑이 Jackson 3 를 직접 다루는지 확실하지 않아 문자열로 받고 서비스가 변환한다. `ddl-auto: validate` 는 `@JdbcTypeCode(SqlTypes.JSON)` 으로 통과한다 — 통합 테스트가 기동 시 확인한다.
- **섹션 필드 키는 두 곳이 같아야 한다** — `ResumeContent` 의 record 컴포넌트 이름과 `types/resume.ts` `SECTIONS[].fields[].key`. Jackson 3 는 모르는 키를 조용히 버리므로 오타가 나면 저장은 되는데 값이 사라진다. `resume.spec.ts` 가 `emptyContent()` 의 키를 `SECTIONS` 와 맞춰 본다.
- 편집기는 **드로어가 아니라 전체 페이지**다(design-system 규칙 7). 520px 에 9개 표를 넣을 수 없다.
- 행 카드의 `:key` 는 index 다. 값은 `:value` + `@input` 로 묶여 있어 ↑↓ 이동 뒤에도 입력값이 따라간다. `v-model` 로 바꾸면 index 키와 충돌해 값이 남는다.
- 목록 응답에는 `content` 가 없다(요약). 카드는 이름·수정일만 그린다.
- **MCP 행 도구는 다른 섹션 필드를 거부한다**(`UNKNOWN_ROW_FIELD`). 합집합 record 로 받기 때문에 스키마상으로는 `school` 을 `certificates` 에 넣을 수 있어 보이지만, 서버가 섹션 record 로 엄격 변환한다. 조용히 버리면 값이 사라진 채 "성공" 이 되기 때문이다.
- 섹션을 추가하면 `ResumeContent`(record) · `ResumeSection`(enum) · `types/resume.ts`(SECTIONS) · `ResumeRowRequest`(새 필드가 있으면) 네 곳을 함께 고친다.
