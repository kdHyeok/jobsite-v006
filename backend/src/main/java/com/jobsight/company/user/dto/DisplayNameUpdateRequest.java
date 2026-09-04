package com.jobsight.company.user.dto;

import jakarta.validation.constraints.Size;

/** 빈 문자열이나 null 은 "이름 지우기" 로 처리된다. */
public record DisplayNameUpdateRequest(
        @Size(max = 80, message = "이름은 80자 이하여야 합니다.")
        String displayName
) {
}
