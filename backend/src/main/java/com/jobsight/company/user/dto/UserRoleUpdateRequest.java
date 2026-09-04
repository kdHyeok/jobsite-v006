package com.jobsight.company.user.dto;

import com.jobsight.company.user.UserRole;
import jakarta.validation.constraints.NotNull;

public record UserRoleUpdateRequest(
        @NotNull(message = "권한은 필수입니다.")
        UserRole role
) {
}
