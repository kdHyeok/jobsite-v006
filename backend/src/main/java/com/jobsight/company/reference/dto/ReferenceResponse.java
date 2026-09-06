package com.jobsight.company.reference.dto;

import com.jobsight.company.reference.ReferenceItem;
import com.jobsight.company.reference.ReferenceKind;

import java.time.Instant;
import java.util.UUID;

public record ReferenceResponse(
        UUID id,
        ReferenceKind kind,
        String title,
        String url,
        String memo,
        UUID relatedPositionId,
        Instant createdAt,
        Instant updatedAt
) {
    public static ReferenceResponse of(ReferenceItem item) {
        return new ReferenceResponse(item.getId(), item.getKind(), item.getTitle(), item.getUrl(), item.getMemo(),
                item.getRelatedPositionId(), item.getCreatedAt(), item.getUpdatedAt());
    }
}
