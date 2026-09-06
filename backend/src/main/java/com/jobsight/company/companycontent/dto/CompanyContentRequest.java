package com.jobsight.company.companycontent.dto;

import com.jobsight.company.companycontent.CompanyContentKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CompanyContentRequest(
        @NotNull(message = "종류는 필수입니다.")
        CompanyContentKind kind,
        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 200, message = "제목은 200자 이하여야 합니다.")
        String title,
        @Size(max = 5000, message = "본문 미리보기는 5,000자 이하여야 합니다.")
        String preview,
        @Size(max = 160, message = "신문사 또는 채널은 160자 이하여야 합니다.")
        String source,
        @NotBlank(message = "링크는 필수입니다.")
        @Size(max = 500, message = "링크는 500자 이하여야 합니다.")
        @Pattern(regexp = "https?://.+", message = "링크는 http 또는 https URL이어야 합니다.")
        String url
) {
}
