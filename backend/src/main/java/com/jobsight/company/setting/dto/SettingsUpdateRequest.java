package com.jobsight.company.setting.dto;

import jakarta.validation.constraints.NotNull;

public record SettingsUpdateRequest(
        @NotNull(message = "자동 승인 여부는 필수입니다.")
        Boolean autoApproveSignup
) {
}
