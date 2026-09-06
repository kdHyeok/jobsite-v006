package com.jobsight.company.position.dto;

import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record PositionReferencesRequest(
        @Size(max = 50, message = "참고 정보는 50개 이하여야 합니다.")
        List<UUID> referenceIds
) {
}
