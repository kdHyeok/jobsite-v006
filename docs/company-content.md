# 기업 뉴스 · 유튜브 자료

## 목적

기업을 조사하며 찾은 뉴스 기사와 유튜브 영상을 기업별 앨범으로 저장·수정·삭제·조회한다.
외부 콘텐츠를 자동 수집하지 않고 사용자가 확인한 링크와 미리보기만 기록한다.

## 진입점

| 무엇 | 어디 |
|---|---|
| 스키마 | `db/migration/V11__company_contents.sql` |
| 백엔드 | `companycontent/CompanyContentController` → `CompanyContentService` |
| 프런트 | 기업 상세 드로어의 `CompanyContentAlbum.vue` |
| API 경로 | 백엔드 `ApiPaths.COMPANY_CONTENTS`, 프런트 `routes.ts` 의 `API.companyContents` |

## 불변 조건

1. 콘텐츠는 기업에 1:N으로 속하고 `owner_id`도 직접 가진다. 모든 조회는 회사와 콘텐츠 모두 현재 계정 소유 조건을 쿼리에 포함한다. 남의 ID는 404.
2. 종류는 `NEWS` 또는 `YOUTUBE`. 제목·링크는 필수이고, 미리보기 본문과 출처는 선택이다.
3. 기업을 삭제하면 그 기업의 콘텐츠도 FK `ON DELETE CASCADE`로 삭제된다.
4. 링크는 `http` 또는 `https`만 받는다. 서버가 외부 URL에 접속하거나 본문·썸네일을 가져오지 않는다.

## API

```text
GET/POST       /api/companies/{companyId}/contents
PUT/DELETE     /api/companies/{companyId}/contents/{id}
```

목록은 최근 수정 순이다. 요청은 `{kind,title,preview,source,url}`이고 응답에 `id,createdAt,updatedAt`이 더해진다.

## 실행·검증

```bash
cd backend && ./gradlew compileJava compileTestJava
cd frontend && npm run type-check && npm test
bash scripts/smoke.sh
```

로그인 뒤 기업 상세에서:
1. `뉴스/유튜브` 탭 → 자료 추가 → 앨범 카드가 생기는가
2. 종류·제목·미리보기·신문사/채널·링크를 수정하면 같은 카드가 갱신되는가
3. 삭제 확인 뒤 카드가 사라지는가
4. 다른 계정의 기업·콘텐츠 ID가 404인가

## 함정

- 뉴스와 유튜브를 별도 테이블·화면으로 나누지 않는다. 필드가 같으므로 `kind` 하나로 구분한다.
- `reference_items`는 여러 직무가 공유하는 참고 정보다. 기업에 종속된 콘텐츠와 수명·소유 의미가 달라 재사용하지 않는다.
- 링크는 새 탭으로만 연다. iframe 임베드와 외부 메타데이터 수집은 제공하지 않는다.
