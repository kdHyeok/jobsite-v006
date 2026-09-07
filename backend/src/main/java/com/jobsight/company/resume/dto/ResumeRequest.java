package com.jobsight.company.resume.dto;

import com.jobsight.company.resume.ResumeContent;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 생성·저장 공용. content 가 null 이면 빈 문서. */
public record ResumeRequest(
        @NotBlank(message = "이력서 이름은 필수입니다.")
        @Size(max = 120, message = "이력서 이름은 120자 이하여야 합니다.")
        String name,
        @Valid ResumeContent content
) {
}
