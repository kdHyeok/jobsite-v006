package com.jobsight.company.selfintro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SelfIntroductionRequest(
        @NotNull(message = "이력서는 필수입니다.") UUID resumeId,
        @NotBlank(message = "질문은 필수입니다.")
        @Size(max = 1000, message = "질문은 1,000자 이하여야 합니다.") String question,
        @Size(max = 10000, message = "답변은 10,000자 이하여야 합니다.") String answer
) {
}
