# 관리자 요청과 피드백

## 목적

로그인 사용자가 화면 우측 하단에서 버그 제보·기능 제안을 관리자에게 보내고 자신의 요청을 수정·삭제한다.
관리자는 관리자 페이지에서 전체 요청을 확인·삭제하고 피드백을 작성·수정·삭제한다.

## 진입점

| 무엇 | 어디 |
|---|---|
| 사용자 API | `adminrequest/AdminRequestController` — `ApiPaths.REQUESTS`, 항상 현재 계정 소유 행만 조회 |
| 관리자 API | `adminrequest/AdminRequestAdminController` — `ApiPaths.ADMIN_REQUESTS`, `SecurityConfig`의 ADMIN 보호 아래 |
| 규칙·트랜잭션 | `adminrequest/AdminRequestService` |
| 저장 | `admin_requests`, 생성 제한 상태는 `app_users`의 요청 횟수·날짜·마지막 시각 |
| 사용자 화면 | `AdminRequestWidget.vue` — 로그인 뒤 모든 화면 우측 하단 |
| 관리자 화면 | `AdminView.vue`의 `요청 관리` 탭 |

## 불변 조건

1. 일반 사용자는 `findByIdAndOwnerId`와 `findAllByOwnerId…`로 자기 요청만 본다. 타인 ID는 404다.
2. 관리자가 타인의 내용을 보는 예외는 관리자 요청 본문·피드백에만 한정한다. 기업·공고 등 기존 소유 데이터는 열지 않는다.
3. 신규 생성은 계정별 5초에 한 번, 한국 시간 기준 하루 50회다. 사용자 행을 비관적 잠금한 뒤 횟수를 소비해 동시 요청도 제한한다. 수정은 생성 횟수로 세지 않는다.
4. 요청 삭제 후에도 같은 날 생성 횟수와 마지막 생성 시각은 계정에 남아 삭제 후 재생성으로 제한을 우회할 수 없다.
5. 관리자 피드백을 새로 작성하거나 수정하면 사용자에게 미확인으로 표시한다. 사용자가 `피드백 확인`을 열면 읽음으로 바꾼다. 피드백 삭제는 요청을 삭제하지 않는다.
6. 요청 종류는 `BUG_REPORT`, `FEATURE_REQUEST`; 요청·피드백 본문은 공백 제외 5~2000자다.

## 실행·검증

```bash
MSYS_NO_PATHCONV=1 docker run --rm -v jobsite-v006-gradle-cache:/root/.gradle \
  -v "$PWD":/workspace -w /workspace/backend eclipse-temurin:21-jdk-alpine \
  ./gradlew compileJava compileTestJava test --tests '*AdminRequest*Test' --no-daemon
cd frontend && npm run type-check && npm test
docker compose up -d --build && bash scripts/smoke.sh
```

브라우저에서는 사용자 생성→수정→관리자 피드백→사용자 미확인 표시·확인→양쪽 삭제와 모바일 창 배치를 확인한다.
마이그레이션 적용은 backend 로그의 `now at version v16`으로 확인한다.

## 함정

- 5초 제한을 프런트 버튼 비활성화만으로 구현하지 않는다. API 직접 호출과 동시 요청을 막아야 한다.
- 삭제된 요청 수를 세면 제한을 우회할 수 있다. 생성 제한 상태는 요청 행과 분리해 계정에 유지한다.
- 사용자 수정 시 기존 피드백을 지우지 않는다. 관리자가 필요하면 피드백을 수정·삭제한다.
- 사용자 창은 비모달 고정 패널이다. 키보드 포커스를 가두지 않고 Escape로 닫으며 작은 화면에서는 좌우 여백을 유지한다.
