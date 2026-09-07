package com.jobsight.company.resume.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResumeCopyRequest(
        @NotBlank(message = "새 버전 이름은 필수입니다.")
        @Size(max = 120, message = "이력서 이름은 120자 이하여야 합니다.")
        String name
) {
}
