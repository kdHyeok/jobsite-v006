package com.jobsight.company.posting.dto;

import com.jobsight.company.posting.ApplicationStatus;
import com.jobsight.company.posting.EmploymentType;
import com.jobsight.company.posting.JobPosting;
import com.jobsight.company.position.dto.PositionSummary;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * D-day 는 담지 않는다. 프런트가 deadlineAt 을 사용자 시계로 계산한다.
 * positions 는 요약(id·name·team…)만. 직무 상세는 /api/positions/{id}.
 */
public record JobPostingResponse(
        UUID id,
        UUID companyId,
        String companyName,
        String title,
        String postingUrl,
        EmploymentType employmentType,
        Instant deadlineAt,
        ApplicationStatus status,
        String qualifications,
        UUID targetPositionId,
        List<StepResponse> steps,
        List<PositionSummary> positions,
        boolean archived,
        Instant createdAt,
        Instant updatedAt
) {
    public static JobPostingResponse of(JobPosting posting, String companyName, List<PositionSummary> positions) {
        List<StepResponse> steps = IntStream.range(0, posting.getSteps().size())
                .mapToObj(i -> StepResponse.of(i, posting.getSteps().get(i)))
                .toList();
        return new JobPostingResponse(
                posting.getId(),
                posting.getCompanyId(),
                companyName,
                posting.getTitle(),
                posting.getPostingUrl(),
                posting.getEmploymentType(),
                posting.getDeadlineAt(),
                posting.getStatus(),
                posting.getQualifications(),
                posting.getTargetPositionId(),
                steps,
                positions,
                posting.isArchived(),
                posting.getCreatedAt(),
                posting.getUpdatedAt()
        );
    }
}
