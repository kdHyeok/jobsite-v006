package com.jobsight.company.position.dto;

import com.jobsight.company.position.Position;
import com.jobsight.company.posting.ApplicationStatus;
import com.jobsight.company.posting.JobPosting;
import com.jobsight.company.reference.dto.ReferenceResponse;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** 직무 + 소속 공고·기업의 요약 + 참고 정보. 직무 페이지 카드와 드로어가 이것 하나로 그린다. */
public record PositionResponse(
        UUID id,
        UUID postingId,
        String postingTitle,
        UUID companyId,
        String companyName,
        Instant deadlineAt,
        ApplicationStatus status,
        boolean archived,
        String name,
        String team,
        String role,
        String responsibilities,
        String impact,
        String growth,
        String experience,
        String requiredSkills,
        String preferredSkills,
        String headcount,
        String workLocation,
        List<String> techStack,
        List<ReferenceResponse> references,
        Instant createdAt,
        Instant updatedAt
) {
    public static PositionResponse of(Position position, JobPosting posting, String companyName,
                                      List<ReferenceResponse> references) {
        return new PositionResponse(
                position.getId(),
                position.getPostingId(),
                posting == null ? null : posting.getTitle(),
                posting == null ? null : posting.getCompanyId(),
                companyName,
                posting == null ? null : posting.getDeadlineAt(),
                posting == null ? null : posting.getStatus(),
                posting != null && posting.isArchived(),
                position.getName(),
                position.getTeam(),
                position.getRole(),
                position.getResponsibilities(),
                position.getImpact(),
                position.getGrowth(),
                position.getExperience(),
                position.getRequiredSkills(),
                position.getPreferredSkills(),
                position.getHeadcount(),
                position.getWorkLocation(),
                List.copyOf(position.getTechStack()),
                references,
                position.getCreatedAt(),
                position.getUpdatedAt()
        );
    }

    /** 검색어(소문자)가 이름·팀·회사·공고 제목·스택 어딘가에 들어 있나. */
    public boolean matches(String needle) {
        return contains(name, needle) || contains(team, needle) || contains(companyName, needle)
                || contains(postingTitle, needle)
                || techStack.stream().anyMatch(t -> contains(t, needle));
    }

    private static boolean contains(String haystack, String needle) {
        return haystack != null && haystack.toLowerCase(Locale.ROOT).contains(needle);
    }
}
