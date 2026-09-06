package com.jobsight.company.posting.dto;

import com.jobsight.company.posting.StepResult;
import jakarta.validation.constraints.NotNull;

public record StepResultUpdateRequest(
        @NotNull(message = "단계 결과는 필수입니다.")
        StepResult result
) {
}
