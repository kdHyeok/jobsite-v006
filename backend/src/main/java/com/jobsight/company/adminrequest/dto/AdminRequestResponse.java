package com.jobsight.company.adminrequest.dto;

import com.jobsight.company.adminrequest.AdminRequest;
import com.jobsight.company.adminrequest.AdminRequestKind;

import java.time.Instant;
import java.util.UUID;

public record AdminRequestResponse(UUID id, AdminRequestKind kind, String message, String feedback,
                                   Instant feedbackUpdatedAt, boolean feedbackUnread,
                                   Instant createdAt, Instant updatedAt) {
    public static AdminRequestResponse of(AdminRequest request) {
        return new AdminRequestResponse(request.getId(), request.getKind(), request.getMessage(),
                request.getFeedback(), request.getFeedbackUpdatedAt(), request.isFeedbackUnread(),
                request.getCreatedAt(), request.getUpdatedAt());
    }
}
