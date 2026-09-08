package com.jobsight.company.common;

import java.util.UUID;

/**
 * 소유자별 집계 한 줄. `group by owner_id` 쿼리의 결과 타입이다.
 * 개수만 담는다 — 관리자에게 다른 계정의 내용을 보여주지 않기 위해서다(docs/admin.md 불변 조건 1).
 */
public record OwnerCount(UUID ownerId, long count) {
}
