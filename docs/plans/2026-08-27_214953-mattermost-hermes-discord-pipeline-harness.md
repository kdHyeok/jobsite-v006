# Mattermost→수집기→큐→공고 파서→Hermes→Discord 파이프라인 실행 하네스/운영 복구 설계

> **For Hermes:** 구현 시 FastAPI + SQLite + 비동기 워커 단일 노드 구성을 유지하고, 모든 단계는 재시도 가능·재실행 가능·관측 가능해야 한다.

**Goal:** Mattermost Outgoing Webhook에서 들어온 공고/URL 요청을 안전하게 수집·파싱·Hermes 처리·Discord 게시까지 연결하되, 중복·실패·부분 장애·운영자 개입 상황에서도 유실 없이 복구 가능한 실행 하네스를 정의한다.

**Architecture:** FastAPI API 프로세스(ingress/admin) + SQLite(상태/큐/아웃박스/감사로그) + asyncio 워커(fetch/parse/hermes/discord) 구조를 기본으로 한다. Kafka/Spark 없이도 SQLite 기반 상태 머신, lease, outbox, replay, dead-letter, shadow/canary를 제공해 운영 복원력을 확보한다.

**Tech Stack:** FastAPI, Pydantic, httpx, aiosqlite/SQLAlchemy, asyncio task group, Prometheus/OpenTelemetry, Discord bot/webhook client, Hermes internal webhook, cron 보조 잡.

---

## 1. 목표 범위와 비범위

### 범위
- Mattermost Outgoing Webhook token 검증.
- 어댑터가 내부 Hermes webhook으로 HMAC 서명 전달.
- Discord 대상 채널: `1542177514347044994`.
- SQLite 기반 durable queue/state machine.
- fetch sandbox, SSRF 차단, replay, dead-letter, dry-run/shadow/canary.
- 운영용 admin/recovery API와 cron 보조 역할.

### 비범위
- 분산 메시지 브로커 도입(Kafka, RabbitMQ).
- 대규모 수평 확장.
- 장기 분석용 데이터 레이크.

---

## 2. 논리 구성요소

1. **Mattermost Adapter (ingress)**
   - Mattermost webhook 수신.
   - 토큰 검증, 요청 정규화, `event_id` 생성.
   - 내부 Hermes webhook 호출 시 `X-Hermes-Signature`(HMAC-SHA256), `X-Request-Id`, `X-Source: mattermost` 전달.

2. **Collector API**
   - ingress payload를 `ingest_event`, `job`, `job_attempt` 레코드로 저장.
   - 즉시 202 응답.
   - 파서 대상 URL/텍스트 후보를 canonicalize.

3. **SQLite Queue/State Store**
   - 별도 브로커 대신 DB가 source-of-truth.
   - `jobs` 테이블이 현재 상태, `job_attempts`가 실행 이력, `outbox`가 외부 전송 보장 역할.

4. **Workers**
   - `fetch-worker`
   - `parse-worker`
   - `hermes-worker`
   - `discord-worker`
   - 각 워커는 lease 기반 polling으로 자신의 단계만 소비.

5. **Admin/Recovery API**
   - replay, requeue, dead-letter 조회, poison job 격리, shadow 비교.

6. **Observability Stack**
   - 구조화 로그 + metrics + traces + audit events.

---

## 3. 데이터 모델(핵심)

### 3.1 `ingest_events`
- `id` (ULID)
- `source` = `mattermost`
- `source_event_key` = Mattermost channel_id + post_id(or timestamp hash)
- `request_id`
- `received_at`
- `token_valid` (bool)
- `hmac_valid` (internal hop 검증용)
- `normalized_payload_json`
- `status` = accepted/rejected/duplicated
- unique(`source`, `source_event_key`)

### 3.2 `jobs`
- `id` (ULID)
- `ingest_event_id`
- `pipeline_key` : dedupe key
- `mode` = active/shadow/dry_run/canary
- `state` (아래 상태 머신)
- `next_attempt_at`
- `attempt_count`
- `max_attempts`
- `lease_owner`
- `lease_expires_at`
- `priority`
- `url_canonical`
- `raw_text`
- `parsed_fingerprint`
- `discord_message_key`
- `last_error_code`
- `last_error_detail`
- `created_at`, `updated_at`, `completed_at`
- unique(`pipeline_key`, `mode`)

### 3.3 `job_attempts`
- `id`
- `job_id`
- `stage` = fetch/parse/hermes/discord
- `attempt_no`
- `started_at`, `finished_at`
- `worker_id`
- `input_snapshot_json`
- `output_snapshot_json`
- `success` (bool)
- `error_class`, `error_code`, `error_detail`
- `duration_ms`

### 3.4 `artifacts`
- `job_id`
- `kind` = raw_fetch/html/text/parse_result/hermes_result/discord_payload/shadow_diff
- `blob_path` or inline text/json
- `sha256`
- `created_at`

### 3.5 `outbox`
- `id`
- `job_id`
- `target` = hermes/discord
- `dedupe_key`
- `payload_json`
- `status` = pending/sent/acked/failed/dead
- `next_attempt_at`
- `attempt_count`
- `last_error_code`
- unique(`target`, `dedupe_key`)

### 3.6 `dead_letters`
- `job_id`
- `failed_stage`
- `terminal_reason`
- `first_failed_at`
- `last_failed_at`
- `replay_count`
- `operator_note`

---

## 4. 상태 머신

`RECEIVED → FETCH_PENDING → FETCHING → FETCHED → PARSE_PENDING → PARSING → PARSED → HERMES_PENDING → HERMES_PROCESSING → HERMES_DONE → DISCORD_PENDING → DISCORD_SENDING → POSTED`

예외/운영 상태:
- `RETRY_WAIT`
- `DEAD_LETTER`
- `CANCELLED`
- `SHADOW_DONE`
- `DRY_RUN_DONE`
- `CANARY_HOLD`

### 전이 규칙
1. `RECEIVED` : ingress 저장 완료.
2. URL이 있으면 `FETCH_PENDING`, 텍스트만 있으면 바로 `PARSE_PENDING`.
3. 각 실행 상태(`FETCHING`, `PARSING`, `HERMES_PROCESSING`, `DISCORD_SENDING`)는 lease를 가진 워커만 진입.
4. 단계 성공 시 다음 pending 상태로 전이.
5. 실패 시 재시도 가능 오류는 `RETRY_WAIT` + `next_attempt_at` 설정 후 동일 stage의 pending으로 복귀.
6. 최대 재시도 초과, 정책 위반, poison payload는 `DEAD_LETTER`.
7. `dry_run`은 Discord 전송 없이 `DRY_RUN_DONE`.
8. `shadow`는 주 경로와 별개로 실행 후 `SHADOW_DONE`, 운영자 비교만 수행.
9. `canary`는 Hermes 결과 또는 Discord payload를 hold queue에 보관하고 샘플 승인 시 `DISCORD_PENDING`으로 진입.

### lease/동시성 규칙
- 워커는 `WHERE state in (...) AND next_attempt_at <= now() AND (lease_expires_at IS NULL OR lease_expires_at < now())` 조건으로 job를 N개 선점.
- lease TTL 예: 5분.
- heartbeat로 연장, 워커 비정상 종료 시 TTL 만료 후 다른 워커가 재획득.
- 동일 job에 대해 active stage는 1개만 허용.

---

## 5. Idempotency 설계

### 5.1 이벤트 레벨
- Mattermost ingress dedupe key: `source_event_key`.
- 같은 webhook이 재전송되어도 `ingest_events` unique 충돌 시 기존 `job_id` 반환.

### 5.2 파이프라인 레벨
- `pipeline_key = sha256(canonical_url || normalized_text || source_channel || parser_version_major)`.
- 동일 공고 재유입 시 active 모드에서는 기존 미완료/완료 job 재사용 또는 no-op 처리.
- parser major version이 바뀌면 새 키를 허용해 재처리 가능.

### 5.3 외부 호출 레벨
- Hermes 호출: `Idempotency-Key = job_id + stage + hermes_prompt_version`.
- Discord 게시: `dedupe_key = job_id + channel_id + rendered_message_sha256`.
- Discord가 자체 idempotency를 지원하지 않으면, 전송 전 `outbox` unique로 중복 전송을 차단하고 성공 후 `discord_message_id` 저장.

### 5.4 replay 레벨
- replay는 기본적으로 새 `job_id`, 같은 `pipeline_key`, `mode=replay` 또는 `replay_seq` 증가로 분기.
- “원본 덮어쓰기 replay”는 금지. 비교 가능성을 위해 lineage를 남긴다.

---

## 6. Retry / Dead-letter 정책

### 오류 분류
1. **Permanent**
   - Mattermost token/HMAC 불일치
   - SSRF 정책 위반
   - URL 스킴 불가
   - parser가 필수 필드 불충족 + 재시도 무의미
   - Discord 4xx(권한/채널 없음/본문 규칙 위반)
2. **Transient**
   - fetch timeout, 429, 5xx
   - Hermes timeout/temporary overload
   - Discord 429/5xx
   - SQLite `database is locked` 일시 충돌
3. **Operator-actionable**
   - HTML 구조 급변
   - 인증/쿠키 필요
   - 메시지 포맷 정책 결정 필요

### backoff
- 기본: exponential backoff + jitter.
- 예시: 30s, 2m, 10m, 30m, 2h, 6h.
- stage별 `max_attempts`
  - fetch: 6
  - parse: 3
  - hermes: 4
  - discord: 8

### dead-letter 진입 조건
- `attempt_count >= max_attempts`
- permanent error
- 동일 stage에서 같은 `error_code` 3회 연속 + payload fingerprint 동일
- 전체 체류 시간 SLA 초과(예: 24h)

### dead-letter 운영 절차
- `/admin/dead-letters` 목록 조회
- 원인별 filter (`SSRF_BLOCKED`, `PARSER_SCHEMA_MISMATCH`, `DISCORD_403` 등)
- 조치 후 `requeue_from_stage(job_id, stage)` 또는 `replay(job_id, overrides)` 수행
- operator note와 재처리 사유 기록

---

## 7. Fetch Sandbox / SSRF 방어

### 허용 정책
- 기본 deny, allowlist 우선.
- `http`, `https`만 허용.
- URL canonicalization 후 검사.
- DNS resolve 결과가 아래면 차단:
  - loopback (`127.0.0.0/8`, `::1`)
  - RFC1918 private ranges
  - link-local, multicast, unspecified
  - cloud metadata (`169.254.169.254` 등)
  - Tailscale/사설 overlay 대역(운영 정의값)
- redirect도 hop마다 동일 정책 재검사.

### 실행 격리
- fetch worker는 별도 저권한 프로세스/컨테이너로 분리 권장.
- outbound egress는 80/443만 허용.
- 파일 스킴, gopher, ftp, data URL 금지.
- 응답 크기 상한(예: 5MB), read timeout, connect timeout 분리.
- 콘텐츠 타입 allowlist(`text/html`, `text/plain`, 제한적 `application/pdf`).

### 증적 보존
- 최종 URL, redirect chain, resolved IP, response headers 일부, body sha256 저장.
- 원문 전체 저장이 부담되면 샘플+hash 보존.

---

## 8. Observability

### 로그
모든 로그는 JSON 구조화:
- `timestamp`
- `level`
- `job_id`
- `ingest_event_id`
- `request_id`
- `stage`
- `state_from`, `state_to`
- `attempt_no`
- `latency_ms`
- `error_code`
- `worker_id`
- `mode`

### 메트릭
- `pipeline_jobs_total{state,mode}`
- `pipeline_stage_duration_seconds{stage}` histogram
- `pipeline_stage_failures_total{stage,error_code}`
- `pipeline_retries_total{stage}`
- `pipeline_dead_letters_total{stage,reason}`
- `pipeline_queue_depth{stage}`
- `pipeline_lease_stolen_total`
- `pipeline_shadow_diff_total{diff_type}`
- `pipeline_canary_approved_total`
- `discord_post_total{channel_id="1542177514347044994",status}`

### 트레이싱
- ingress request → fetch → parse → hermes → discord를 trace 하나로 연결.
- `request_id`와 `job_id`를 trace baggage에 넣는다.
- 외부 호출(httpx) span에 status code, retry count, endpoint class 기록.

### 경보
- dead-letter rate 급증
- fetch 429/403 급증
- parse schema mismatch 연속 발생
- Hermes latency p95 초과
- Discord 429 지속 발생
- queue depth/SLA 체류 시간 초과

---

## 9. Replay / Recovery 인터페이스

### admin API 예시
- `POST /admin/jobs/{job_id}/requeue?stage=parse`
- `POST /admin/jobs/{job_id}/replay`
- `POST /admin/jobs/replay-range`
- `POST /admin/jobs/{job_id}/cancel`
- `GET /admin/jobs/{job_id}/timeline`
- `GET /admin/dead-letters`
- `GET /admin/shadow-diffs`

### recovery 규칙
- 재시도와 replay를 구분.
  - **retry**: 같은 job_id, 같은 stage 재개.
  - **replay**: 새 lineage 생성, 코드/프롬프트 버전이 달라도 비교 가능.
- replay input은 원본 artifact에서 읽고, live source 재호출은 옵션화.
- Discord 이미 게시된 job replay는 기본 dry-run으로 수행하고, 운영자 승인 후만 실제 재게시.

### 운영자 화면/CLI 최소 요구
- job timeline 시각화
- stage별 마지막 에러와 artifact 링크
- “fetch 결과는 고정, parse부터 다시” 버튼
- “Hermes prompt version v3로 shadow replay” 버튼

---

## 10. Golden Dataset

### 목적
- 사이트 구조 변경, parser 회귀, Hermes 프롬프트 드리프트, Discord 렌더링 회귀를 조기 탐지.

### 구성
최소 30~50건으로 시작:
1. 정상 HTML 공고
2. 텍스트만 있는 Mattermost 입력
3. redirect 포함 URL
4. robots/403/429 사례
5. malformed HTML
6. 중복 공고
7. 다국어/한글 깨짐 가능 사례
8. 긴 본문/긴 제목
9. PDF 공고
10. SSRF 유사 악성 URL 샘플

### 저장물
각 golden case에 대해:
- ingress payload
- canonical URL/text
- expected parse JSON
- expected Hermes normalized output
- expected Discord payload snapshot
- expected policy outcome(accept/retry/dead-letter/block)

### 검증 방식
- parser 단위 테스트
- end-to-end replay 테스트
- snapshot diff 테스트
- shadow 결과가 baseline과 얼마나 다른지 diff report 생성

---

## 11. Shadow / Dry-run / Canary

### dry-run
- 모든 단계를 실행하되 Discord 전송 직전 종료.
- 운영자가 메시지 preview와 diff를 확인.
- 신규 parser/Hermes prompt 릴리즈 전 기본 모드.

### shadow
- live active job와 동일 입력을 별도 `mode=shadow`로 병렬 실행.
- active는 실제 Discord 게시, shadow는 결과 비교만 수행.
- diff 항목:
  - 추출 필드 누락/추가
  - 요약 품질 지표(길이, 금칙어, 링크 보존)
  - Discord 렌더링 길이 초과 여부

### canary
- 예: 전체 traffic의 5%만 새 parser/Hermes version 사용.
- 또는 특정 source/channel/time window만 적용.
- canary failure threshold 예시:
  - parse 실패율 > baseline + 5%p
  - dead-letter 3건 연속
  - operator rejection 2건
- threshold 초과 시 자동 rollback하여 stable version으로 복귀.

---

## 12. Cron 보조 역할

cron은 주 실행기가 아니라 **보조 복구/정리/검증자**로 사용한다.

1. **lease sweeper** (매분)
   - 만료된 lease 해제
   - stuck 상태를 pending/retry_wait로 복귀

2. **retry kicker** (매분)
   - `next_attempt_at <= now()` 인 job를 깨움
   - 워커가 polling하지만 cron이 보조적으로 starvation 방지

3. **dead-letter digest** (매 10분)
   - 운영자용 요약 생성, 필요 시 Discord/로그 전송

4. **golden replay** (배포 후/매일)
   - golden dataset shadow replay
   - diff/회귀 리포트 생성

5. **artifact retention** (매일)
   - 오래된 raw artifact 압축/삭제
   - dead-letter 관련 증적은 더 오래 유지

6. **canary judge** (매 5분)
   - canary/shadow metrics 읽고 승격/중단 결정

7. **queue vacuum/health** (매일)
   - SQLite VACUUM/ANALYZE는 저부하 시간대 수행
   - DB lock/size/SLA 점검

---

## 13. 운영 Runbook 핵심 시나리오

### 시나리오 A: Mattermost 중복 전달
- ingress unique 충돌로 기존 `ingest_event` 반환.
- 로그에 `dedupe_hit=true` 기록.
- downstream 신규 job 생성 금지.

### 시나리오 B: fetch 대상 사이트 일시 장애
- `FETCH_PENDING ↔ RETRY_WAIT` 반복.
- backoff 적용.
- 6회 초과 시 dead-letter.
- cron digest가 운영자에게 통보.

### 시나리오 C: parser 회귀 배포
- golden replay/shadow diff가 증가.
- canary judge가 auto rollback.
- active stable parser 유지.

### 시나리오 D: Discord 429
- discord outbox가 전송 지연.
- `Retry-After` 존중.
- queue depth 알람 발생.
- 메시지 순서가 중요하면 channel별 serialization key 사용.

### 시나리오 E: 워커 프로세스 강제 종료
- lease TTL 만료 후 다른 워커가 재획득.
- 동일 attempt는 idempotent하게 재실행.

---

## 14. Acceptance Tests

### A. ingress/security
1. **Mattermost token 불일치**
   - 입력: 잘못된 token
   - 기대: 401/403, `ingest_events` 미생성 또는 rejected 생성, downstream job 없음.
2. **내부 HMAC 불일치**
   - 기대: collector reject, audit log 기록.
3. **중복 webhook 재전송**
   - 기대: 동일 `source_event_key`는 job 1개만 유지.

### B. fetch sandbox/SSRF
4. **loopback URL** (`http://127.0.0.1/...`)
   - 기대: fetch 전 차단, `DEAD_LETTER`, `error_code=SSRF_BLOCKED`.
5. **redirect 후 private IP 이동**
   - 기대: redirect hop 검사에서 차단.
6. **5MB 초과 응답**
   - 기대: fetch abort, retry 아님 또는 정책 기반 permanent 처리.

### C. 상태 머신/재시도
7. **fetch timeout 2회 후 성공**
   - 기대: attempt 3에서 다음 stage 진입, duplicate side effect 없음.
8. **Hermes timeout 후 재시도**
   - 기대: 동일 `Idempotency-Key` 사용, 결과 1회만 반영.
9. **Discord 429**
   - 기대: outbox 재스케줄, 최종적으로 1회만 게시.
10. **worker crash during parse**
   - 기대: lease expiry 후 재처리, state corruption 없음.

### D. dead-letter/recovery
11. **parser schema mismatch 3회 연속**
   - 기대: `DEAD_LETTER` 이동, operator note 가능.
12. **dead-letter에서 parse stage 재큐잉**
   - 기대: fetch artifact 재사용, 새 parse attempt 생성.
13. **posted job replay**
   - 기대: 기본 dry-run, Discord 중복 게시 없음.

### E. shadow/dry-run/canary
14. **dry-run 성공**
   - 기대: Discord API 미호출, preview artifact 생성.
15. **shadow 결과 diff 발생**
   - 기대: `shadow_diff` artifact 저장, active 게시에는 영향 없음.
16. **canary 실패 threshold 초과**
   - 기대: 자동 stable rollback, 이후 신규 job는 stable 경로 사용.

### F. golden dataset
17. **golden replay 전체 통과**
   - 기대: baseline snapshot과 동일.
18. **golden 중 SSRF 샘플**
   - 기대: 항상 block.
19. **긴 Discord 메시지 샘플**
   - 기대: split/truncate 정책이 deterministic.

### G. 운영성
20. **admin timeline 조회**
   - 기대: stage/attempt/artifact 링크/에러 코드 확인 가능.
21. **metrics scrape**
   - 기대: queue depth, dead-letter, latency 메트릭 노출.
22. **cron lease sweeper**
   - 기대: stuck lease 해제 후 재진행.

---

## 15. 구현 우선순위

### Phase 1 (필수)
- ingress token/HMAC 검증
- SQLite jobs/job_attempts/outbox
- 상태 머신 + lease + retry/dead-letter
- fetch sandbox/SSRF
- Discord active path idempotency
- admin timeline/dead-letter/requeue
- 기본 metrics/logging

### Phase 2 (운영 안정화)
- artifact 저장/sha 추적
- replay lineage
- golden dataset
- dry-run/shadow
- cron sweeper/digest

### Phase 3 (안전한 변경 관리)
- canary auto judge
- shadow diff 리포트 자동화
- parser/Hermes version routing
- 운영자 승인 hold queue

---

## 16. 권장 파일 배치

- `docs/plans/2026-08-27_214953-mattermost-hermes-discord-pipeline-harness.md`
- `pipeline/app.py` — FastAPI ingress/admin
- `pipeline/models.py` — SQLite schema
- `pipeline/repository.py` — state transition/lease/outbox
- `pipeline/workers/fetch.py`
- `pipeline/workers/parse.py`
- `pipeline/workers/hermes.py`
- `pipeline/workers/discord.py`
- `pipeline/security/fetch_policy.py`
- `pipeline/replay.py`
- `tests/acceptance/test_pipeline_acceptance.py`
- `tests/golden/*.json`

---

## 17. 구현 시 강제 규칙

1. 상태 전이는 repository 함수 하나를 통해서만 수행.
2. 외부 부작용(Hermes/Discord)은 반드시 outbox 또는 idempotency key를 거친다.
3. fetch 결과와 parse 결과는 artifact/hash를 남겨 replay 근거를 만든다.
4. dead-letter는 숨기지 말고 운영자에게 노출한다.
5. cron은 보조자이며, 핵심 정합성은 DB 상태 머신이 책임진다.
6. acceptance test와 golden replay 없이는 parser/Hermes version 승격 금지.

---

이 설계의 핵심은 **SQLite를 단순 저장소가 아니라 실행 저널 + 복구 제어면(control plane)** 으로 사용하는 것이다. 이 원칙을 지키면 단일 노드 FastAPI/async worker 스택에서도 중복 방지, 재처리, 장애 복구, 안전한 변경 배포가 가능하다.
