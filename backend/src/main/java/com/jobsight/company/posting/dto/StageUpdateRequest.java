package com.jobsight.company.posting.dto;

import com.jobsight.company.posting.ApplicationStage;
import jakarta.validation.constraints.NotNull;

public record StageUpdateRequest(
        @NotNull(message = "지원단계는 필수입니다.")
        ApplicationStage stage
) {
}
