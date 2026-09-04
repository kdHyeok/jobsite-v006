package com.jobsight.company.posting.dto;

import com.jobsight.company.posting.ApplicationStage;
import com.jobsight.company.posting.EmploymentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/**
 * 공고 생성·수정 요청.
 * deadlineAt 은 UTC Instant 로 받는다. 프런트가 datetime-local 값을 toISOString() 으로 변환해 보낸다.
 */
public record JobPostingRequest(
        /** 기업을 연결하면 고용회사명은 기업 이름을 따른다. */
        UUID companyId,

        @Size(max = 120, message = "고용회사는 120자 이하여야 합니다.")
        String companyName,

        @NotBlank(message = "채용직무는 필수입니다.")
        @Size(max = 160, message = "채용직무는 160자 이하여야 합니다.")
        String position,

        @Size(max = 500, message = "공고 링크는 500자 이하여야 합니다.")
        @Pattern(regexp = "^$|https?://.+", message = "공고 링크는 http 또는 https URL이어야 합니다.")
        String postingUrl,

        @NotNull(message = "고용형태는 필수입니다.")
        EmploymentType employmentType,

        /** 비우면 상시채용으로 저장된다. */
        Instant deadlineAt,

        @NotNull(message = "지원단계는 필수입니다.")
        ApplicationStage stage,

        @Size(max = 60, message = "모집인원은 60자 이하여야 합니다.")
        String headcount,

        @Size(max = 160, message = "근무지역은 160자 이하여야 합니다.")
        String workLocation,

        @Size(max = 5000, message = "지원자격은 5,000자 이하여야 합니다.")
        String qualifications,

        @Size(max = 5000, message = "담당업무는 5,000자 이하여야 합니다.")
        String responsibilities,

        @Size(max = 5000, message = "요구역량은 5,000자 이하여야 합니다.")
        String requiredSkills
) {
}
