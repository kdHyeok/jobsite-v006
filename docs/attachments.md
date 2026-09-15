# 첨부파일

## 목적

이력서의 증빙 서류(포트폴리오·활동 자료·수상 증빙·자격증 사본·이수증·졸업증·성적증명서)를 **jpg · png · pdf** 로 올리고, 화면에서 바로 보고, 교체한다.

## 도메인

```
app_users ──1:N──▶ attachments
                     id · owner_id · filename · content_type · size_bytes · data(bytea) · created_at
```

**이력서 행은 첨부의 id 문자열만 들고 있다.** 그래서 `Row` 구조가 그대로 유지되고,
이력서는 여전히 JSON 문서 하나다 — `docs/resumes.md`.

## 불변 조건

1. **소유자 격리**: `findByIdAndOwnerId` 만. 타인 첨부는 404. 바이트도 메타도 나가지 않는다.
2. **타입은 클라이언트 말이 아니라 파일 앞머리로 정한다.** `Content-Type` 헤더와 확장자는 위조된다.
   `AttachmentService.sniff()` 가 매직 바이트(`\x89PNG`, `\xFF\xD8\xFF`, `%PDF-`)로 판정하고, 셋 중 하나가 아니면 415.
   저장하는 `content_type` 도 이 판정 결과다 — 내려줄 때 그대로 쓰기 때문이다.
3. **10MB 상한.** 서버(`spring.servlet.multipart`)·서비스·nginx(`client_max_body_size 12m`) 세 곳이 함께 막는다.
   셋 다 있어야 한다 — nginx 가 없으면 413 대신 연결이 끊기고, 서비스 검사가 없으면 curl 경로가 뚫린다.
4. **내려줄 때 `X-Content-Type-Options: nosniff` + `Content-Disposition: inline`.** 브라우저가 타입을 다시 추측하지 못하게 한다.
5. **파일명은 표시용이다.** 경로 구분자를 떼고 260자로 자른 뒤 저장하며, 어떤 경로 조립에도 쓰지 않는다.

## 진입점

| 무엇 | 어디 |
|---|---|
| 스키마 | `V15__company_businesses_and_attachments.sql` |
| 도메인 | `attachment/{Attachment,AttachmentRepository,AttachmentService,AttachmentController}` — `ApiPaths.ATTACHMENTS` |
| 화면 | `AttachmentField.vue` — 첨부·교체·제거·미리보기 한 컴포넌트. 이력서 필드 `kind: 'file'` 이 이걸 그린다 |
| 프런트 API | `api/attachments.ts` · 경로는 `routes.ts` 의 `API.attachments*` |
| 업로드 한계 | `application.yml` 의 `spring.servlet.multipart` · `frontend/nginx.conf` 의 `client_max_body_size` |

## API

```
POST   /api/attachments        multipart/form-data, part 이름 "file"  → {id, filename, contentType, size}
GET    /api/attachments/{id}          → 바이트 (inline)
GET    /api/attachments/{id}/meta     → {id, filename, contentType, size}
DELETE /api/attachments/{id}
```

MCP 도구는 없다. 바이너리는 JSON 도구 호출에 실을 것이 아니다.

## 실행·검증

```bash
docker compose up -d --build && docker compose logs backend | grep "now at version"   # v15
bash scripts/smoke.sh                       # /api/attachments 미인증 401 포함
cd frontend && npm run type-check && npm test
```

로그인 뒤 브라우저에서:
1. 이력서 → 자격증 행 → `파일 선택` → png 업로드 → 파일명 칩과 `미리보기` 가 뜨는가
2. `미리보기` → 이미지가 뜨고, pdf 는 뷰어로 열리는가
3. `교체` → 다른 파일로 바뀌고 이전 파일은 사라지는가
4. 이력서를 저장하지 않고 나갔다 오면 첨부가 풀려 있는가 (**업로드는 즉시, 연결은 저장 시점**)
5. `.txt` 를 png 로 이름만 바꿔 올리면 415 로 거부되는가
6. 이력서 내보내기에서 선택한 첨부만 원래 파일명으로 ZIP에 들어가는가

## 함정

- **업로드와 이력서 저장은 시점이 다르다.** 파일은 고르는 즉시 서버에 올라가고(그래야 미리보기가 된다),
  그 id 는 `저장` 을 눌러야 이력서 문서에 들어간다. 저장 없이 나가면 **주인 없는 첨부**가 남는다.
  자동 청소는 없다 — 개인 도구에서 몇 개의 고아 파일보다 저장 흐름이 복잡해지는 쪽이 비싸다.
  `ponytail: 고아 첨부는 방치. 용량이 문제가 되면 이력서 전체를 훑는 야간 청소를 붙인다.`
- **교체는 새 업로드 + 이전 것 삭제다.** 같은 id 의 바이트를 덮어쓰지 않는다 — 복제한 이력서가 같은 id 를 가리키고 있어서, 덮어쓰면 원본 이력서의 첨부까지 바뀐다.
- `fetch` 에 `FormData` 를 줄 때 `Content-Type` 을 직접 넣으면 boundary 가 빠져 서버가 파트를 못 읽는다.
  `api/http.ts` 가 `FormData` 를 예외로 두는 이유다.
- bytea 는 행에 같이 붙는다. 메타 조회는 별도 프로젝션(`AttachmentRepository.findMeta`)을 써서 바이트를 읽지 않는다.
