package com.jobsight.company.companycontent.dto;

import com.jobsight.company.companycontent.CompanyContent;
import com.jobsight.company.companycontent.CompanyContentKind;

import java.time.Instant;
import java.util.UUID;

public record CompanyContentResponse(
        UUID id,
        CompanyContentKind kind,
        String title,
        String preview,
        String source,
        String url,
        Instant createdAt,
        Instant updatedAt
) {
    public static CompanyContentResponse of(CompanyContent content) {
        return new CompanyContentResponse(content.getId(), content.getKind(), content.getTitle(),
                content.getPreview(), content.getSource(), content.getUrl(),
                content.getCreatedAt(), content.getUpdatedAt());
    }
}
