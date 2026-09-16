# 자기소개 문항

## 목적

이력서 버전별로 자기소개 질문과 답변을 저장하고, 별도 자기소개 화면에서 계정 안의 문항을 검색·수정·삭제한다.
검색 결과는 검색어를 포함한 단어의 빈도와 문서 길이를 반영한 BM25 유사 점수순이며, 화면은 검색어가 나온 부분을 강조한다.
문항 카드는 수정 버튼뿐 아니라 더블클릭으로도 같은 편집기를 연다.

## 도메인

```
app_users ──1:N──▶ self_introductions ──N:M──▶ resumes
                  id · owner_id · question · answer · created_at · updated_at
                                      via self_introduction_resumes
```

하나의 문항은 같은 계정의 이력서 여러 개에 연결할 수 있다. 질문은 이력서 편집기 맨 아래에서 선택적 답변과 함께 추가하거나 과거 문항 검색으로 연결한다. 연결된 기존 문항은 그 자리에서 질문·답변을 수정할 수 있고, 질문 추가와 과거 검색은 기존 문항 목록 아래에 둔다. `/introductions`에서도 전체 CRUD를 한다.

## 불변 조건

1. 모든 조회·수정·삭제는 `owner_id`가 포함된 repository 메서드만 사용한다. 타인 데이터는 404다.
2. `resumeIds`는 1개 이상이며 연결할 모든 이력서를 같은 소유자인지 한 번에 확인한다.
3. 질문은 1,000자 이하 필수, 답변은 10,000자 이하 선택이다.
4. 검색은 질문과 답변을 Unicode 문자·숫자 단어로 나누고 검색어를 포함한 단어를 서버에서 BM25 유사 점수로 계산한다. 검색어가 없으면 최근 수정순이다.
5. 강조 표시는 텍스트 조각을 Vue 노드로 나누며 `v-html`을 사용하지 않는다.

## 진입점

| 무엇 | 어디 |
|---|---|
| 스키마 | `V19__resume_positions_and_self_introductions.sql` · 다중 이력서 연결 `V20__self_introduction_resumes.sql` |
| 백엔드 | `selfintro/SelfIntroduction*` · `ApiPaths.SELF_INTRODUCTIONS` |
| 화면 | `SelfIntroductionBoard.vue` (`/introductions`) · `ResumeQuestions.vue`(이력서 안에서 문항 추가) |
| 검색·강조 | `SelfIntroductionService.rank()` · `utils/highlight.ts` |
| MCP | `self_intro_list/get/create/update/delete` |

## 실행·검증

```bash
docker compose up -d --build
docker compose logs backend   # now at version v20
bash scripts/smoke.sh
cd frontend && npm run type-check && npm test
```

로그인 뒤 이력서에서 여러 직무를 선택해 저장한 다음 새로고침해 유지되는지 확인한다. 기존 문항의 질문·답변을 이력서에서 수정하고 질문 추가·과거 검색이 목록 아래에 있는지 확인한다. 질문을 추가해 자기소개 화면에서
답변 작성·수정·삭제하고, 여러 이력서를 연결할 수 있는지, 문항 카드 더블클릭으로 편집기가 열리는지, 질문 또는 답변의 단어로 검색했을 때 점수순 결과와 강조 표시가 나오는지 확인한다.

## 함정

- BM25는 계정의 현재 문항 집합 안에서만 IDF를 계산한다. 형태소 분석은 하지 않고 `협업`이 `협업으로`도 찾도록 단어 내 포함 여부를 쓴다.
- 자기소개 문항 생성은 이력서 본문 저장과 별도 요청이다. 성공 응답을 받은 문항만 저장된 것이다.
- 이력서 복제는 연결 직무는 복사하지만 자기소개 문항은 복사하지 않는다. 문항 답변까지 복제하면 서로 다른 지원서가 같은 초안을 갖는 혼동이 생긴다.
