package com.jobsight.company.company.dto;

import jakarta.validation.constraints.Size;

/** 주요 사업 한 건의 입력. 이름이 빈 행은 서비스가 버린다(폼에서 빈 줄을 남기고 저장하는 일이 흔하다). */
public record BusinessAreaRequest(
        @Size(max = 120, message = "사업명은 120자 이하여야 합니다.")
        String name,

        @Size(max = 2000, message = "사업 설명은 2,000자 이하여야 합니다.")
        String description
) {
}
