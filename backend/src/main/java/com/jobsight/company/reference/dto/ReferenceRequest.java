package com.jobsight.company.reference.dto;

import com.jobsight.company.reference.ReferenceKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ReferenceRequest(
        @NotNull(message = "종류는 필수입니다.")
        ReferenceKind kind,
        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 200, message = "제목은 200자 이하여야 합니다.")
        String title,
        @Size(max = 500, message = "링크는 500자 이하여야 합니다.")
        @Pattern(regexp = "^$|https?://.+", message = "링크는 http 또는 https URL이어야 합니다.")
        String url,
        @Size(max = 5000, message = "메모는 5,000자 이하여야 합니다.")
        String memo,
        UUID relatedPositionId
) {
}
