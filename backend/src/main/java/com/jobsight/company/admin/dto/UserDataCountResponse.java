package com.jobsight.company.admin.dto;

import java.util.UUID;

/** 계정 하나가 등록한 데이터 개수. 내용은 담지 않는다. */
public record UserDataCountResponse(UUID userId, long companies, long postings, long positions) {
}
