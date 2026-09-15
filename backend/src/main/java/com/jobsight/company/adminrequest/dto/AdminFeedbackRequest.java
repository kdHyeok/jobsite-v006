package com.jobsight.company.adminrequest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminFeedbackRequest(
        @NotBlank(message = "피드백을 입력해 주세요.")
        @Size(min = 5, max = 2000, message = "피드백은 5자 이상 2000자 이하로 입력해 주세요.") String feedback) {
    public AdminFeedbackRequest {
        if (feedback != null) feedback = feedback.trim();
    }
}
