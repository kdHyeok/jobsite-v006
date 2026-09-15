package com.jobsight.company.adminrequest.dto;

import com.jobsight.company.adminrequest.AdminRequestKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminRequestWriteRequest(
        @NotNull(message = "요청 종류를 선택해 주세요.") AdminRequestKind kind,
        @NotBlank(message = "요청 내용을 입력해 주세요.")
        @Size(min = 5, max = 2000, message = "요청 내용은 5자 이상 2000자 이하로 입력해 주세요.") String message) {
    public AdminRequestWriteRequest {
        if (message != null) message = message.trim();
    }
}
