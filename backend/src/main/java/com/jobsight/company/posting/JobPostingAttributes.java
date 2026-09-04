package com.jobsight.company.posting;

import java.time.Instant;
import java.util.UUID;

/** 정규화가 끝난 공고 속성. 생성·수정이 공통으로 쓴다. */
public record JobPostingAttributes(
        UUID companyId,
        String companyName,
        String position,
        String postingUrl,
        EmploymentType employmentType,
        Instant deadlineAt,
        ApplicationStage stage,
        String headcount,
        String workLocation,
        String qualifications,
        String responsibilities,
        String requiredSkills
) {
}
