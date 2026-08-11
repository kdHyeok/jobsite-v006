package com.jobsight.company.company.dto;

import com.jobsight.company.company.Company;
import com.jobsight.company.company.CompanyStatus;

import java.time.Instant;
import java.util.UUID;

public record CompanyResponse(
        UUID id,
        String name,
        String industry,
        String location,
        String websiteUrl,
        CompanyStatus status,
        String summary,
        String memo,
        Instant createdAt,
        Instant updatedAt
) {
    public static CompanyResponse from(Company company) {
        return new CompanyResponse(
                company.getId(), company.getName(), company.getIndustry(), company.getLocation(),
                company.getWebsiteUrl(), company.getStatus(), company.getSummary(), company.getMemo(),
                company.getCreatedAt(), company.getUpdatedAt()
        );
    }
}
