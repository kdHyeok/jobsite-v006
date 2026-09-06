package com.jobsight.company.posting.dto;

import com.jobsight.company.posting.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(
        @NotNull(message = "지원 상태는 필수입니다.")
        ApplicationStatus status
) {
}
