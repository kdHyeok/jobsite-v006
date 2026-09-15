package com.jobsight.company.adminrequest.dto;

import com.jobsight.company.adminrequest.AdminRequest;
import com.jobsight.company.adminrequest.AdminRequestKind;
import com.jobsight.company.user.AppUser;

import java.time.Instant;
import java.util.UUID;

public record AdminRequestAdminResponse(UUID id, UUID requesterId, String requesterEmail, String requesterName,
                                        AdminRequestKind kind, String message, String feedback,
                                        Instant feedbackUpdatedAt, boolean feedbackUnread,
                                        Instant createdAt, Instant updatedAt) {
    public static AdminRequestAdminResponse of(AdminRequest request, AppUser user) {
        return new AdminRequestAdminResponse(request.getId(), user.getId(), user.getEmail(), user.getDisplayName(),
                request.getKind(), request.getMessage(), request.getFeedback(), request.getFeedbackUpdatedAt(),
                request.isFeedbackUnread(), request.getCreatedAt(), request.getUpdatedAt());
    }
}
