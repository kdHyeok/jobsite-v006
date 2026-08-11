package com.jobsight.company.company.dto;

import com.jobsight.company.company.CompanyStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CompanyCreateRequest(
        @NotBlank(message = "기업명은 필수입니다.")
        @Size(max = 120, message = "기업명은 120자 이하여야 합니다.")
        String name,
        @Size(max = 120, message = "산업 분야는 120자 이하여야 합니다.")
        String industry,
        @Size(max = 160, message = "지역은 160자 이하여야 합니다.")
        String location,
        @Size(max = 500, message = "웹사이트 URL은 500자 이하여야 합니다.")
        @Pattern(regexp = "^$|https?://.+", message = "웹사이트는 http 또는 https URL이어야 합니다.")
        String websiteUrl,
        @NotNull(message = "지원 상태는 필수입니다.")
        CompanyStatus status,
        @Size(max = 2000, message = "요약은 2,000자 이하여야 합니다.")
        String summary,
        @Size(max = 5000, message = "메모는 5,000자 이하여야 합니다.")
        String memo
) {
}
