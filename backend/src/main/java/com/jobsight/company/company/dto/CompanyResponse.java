package com.jobsight.company.company.dto;

import com.jobsight.company.company.Company;
import com.jobsight.company.company.CompanySize;
import com.jobsight.company.posting.dto.JobPostingResponse;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * openPostings 는 사용자가 입력하는 값이 아니라 이 기업에 연결된 공고에서 채워진다.
 * 목록 응답에서는 비어 있고(N+1 방지), 상세 조회에서만 담긴다.
 */
public record CompanyResponse(
        UUID id,
        String name,
        String websiteUrl,
        List<String> industries,
        CompanySize companySize,
        Long annualRevenue,
        Integer employeeCount,
        String address,
        LocalDate foundedOn,
        String summary,
        String memo,
        List<JobPostingResponse> openPostings,
        Instant createdAt,
        Instant updatedAt
) {
    public static CompanyResponse summaryOf(Company company) {
        return of(company, List.of());
    }

    public static CompanyResponse of(Company company, List<JobPostingResponse> openPostings) {
        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getWebsiteUrl(),
                List.copyOf(company.getIndustries()),
                company.getCompanySize(),
                company.getAnnualRevenue(),
                company.getEmployeeCount(),
                company.getAddress(),
                company.getFoundedOn(),
                company.getSummary(),
                company.getMemo(),
                openPostings,
                company.getCreatedAt(),
                company.getUpdatedAt()
        );
    }
}
