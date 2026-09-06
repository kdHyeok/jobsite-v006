package com.jobsight.company.posting.dto;

import com.jobsight.company.posting.StepResult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/** 절차 한 단계. 배열 순서가 곧 진행 순서다. result 를 비우면 UPCOMING. */
public record StepRequest(
        @NotBlank(message = "단계 이름은 필수입니다.")
        @Size(max = 60, message = "단계 이름은 60자 이하여야 합니다.")
        String name,
        StepResult result,
        Instant scheduledAt,
        @Size(max = 2000, message = "메모는 2,000자 이하여야 합니다.")
        String memo
) {
}
