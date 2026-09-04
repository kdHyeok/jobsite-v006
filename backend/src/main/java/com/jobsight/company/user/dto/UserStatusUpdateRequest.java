package com.jobsight.company.user.dto;

import com.jobsight.company.user.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UserStatusUpdateRequest(
        @NotNull(message = "상태는 필수입니다.")
        UserStatus status
) {
}
