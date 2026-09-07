package com.jobsight.company.resume.dto;

import jakarta.validation.constraints.Size;

/**
 * MCP 행 도구의 입력. 8개 섹션 필드의 합집합이다 — MCP 스키마 생성기가 Map 을 그리지 못하고,
 * 섹션마다 도구를 만들면 24개가 된다. 어느 필드가 어느 섹션 것인지는 서비스가 섹션 record 로
 * 엄격 변환하며 검사한다(다른 섹션 필드 → UNKNOWN_ROW_FIELD). 값이 null 인 필드는 보내지 않은 것으로 본다.
 */
public record ResumeRowRequest(
        @Size(max = 200) String name,
        @Size(max = 200) String school,
        @Size(max = 200) String major,
        @Size(max = 200) String gpa,
        @Size(max = 200) String startYm,
        @Size(max = 200) String endYm,
        @Size(max = 200) String institution,
        @Size(max = 200) String organizer,
        @Size(max = 200) String company,
        @Size(max = 200) String issuer,
        @Size(max = 200) String awardedYm,
        @Size(max = 200) String acquiredYm,
        @Size(max = 200) String level,
        @Size(max = 200) String headcount,
        @Size(max = 5000) String summary,
        @Size(max = 500) String techStack,
        @Size(max = 500) String role,
        @Size(max = 5000) String outcome,
        @Size(max = 5000) String description,
        @Size(max = 500) String url
) {
}
