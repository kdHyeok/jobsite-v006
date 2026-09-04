package com.jobsight.company.posting.dto;

import com.jobsight.company.posting.ApplicationStage;
import com.jobsight.company.posting.EmploymentType;
import com.jobsight.company.posting.JobPosting;

import java.time.Instant;
import java.util.UUID;

/**
 * D-day 는 담지 않는다. 프런트가 deadlineAt 을 사용자 시계로 계산한다.
 * 서버가 계산해 내려주면 자정을 넘길 때 화면이 틀어진다.
 */
public record JobPostingResponse(
        UUID id,
        UUID companyId,
        /** 기업을 연결했으면 기업 이름, 아니면 입력한 고용회사명. */
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
        String requiredSkills,
        boolean archived,
        Instant createdAt,
        Instant updatedAt
) {
    public static JobPostingResponse of(JobPosting posting, String resolvedCompanyName) {
        return new JobPostingResponse(
                posting.getId(),
                posting.getCompanyId(),
                resolvedCompanyName,
                posting.getPosition(),
                posting.getPostingUrl(),
                posting.getEmploymentType(),
                posting.getDeadlineAt(),
                posting.getStage(),
                posting.getHeadcount(),
                posting.getWorkLocation(),
                posting.getQualifications(),
                posting.getResponsibilities(),
                posting.getRequiredSkills(),
                posting.isArchived(),
                posting.getCreatedAt(),
                posting.getUpdatedAt()
        );
    }
}
