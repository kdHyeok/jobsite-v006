package com.jobsight.company.adminrequest;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "admin_requests")
public class AdminRequest {
    @Id private UUID id;
    @Column(name = "owner_id", nullable = false, updatable = false) private UUID ownerId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private AdminRequestKind kind;
    @Column(nullable = false, columnDefinition = "TEXT") private String message;
    @Column(columnDefinition = "TEXT") private String feedback;
    @Column(name = "feedback_updated_at") private Instant feedbackUpdatedAt;
    @Column(name = "feedback_read_at") private Instant feedbackReadAt;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected AdminRequest() {}

    public AdminRequest(UUID ownerId, AdminRequestKind kind, String message, Instant now) {
        this.id = UUID.randomUUID();
        this.ownerId = ownerId;
        this.createdAt = now;
        update(kind, message, now);
    }

    public void update(AdminRequestKind kind, String message, Instant now) {
        this.kind = kind;
        this.message = message;
        this.updatedAt = now;
    }

    public void saveFeedback(String feedback, Instant now) {
        this.feedback = feedback;
        this.feedbackUpdatedAt = now;
        this.feedbackReadAt = null;
        this.updatedAt = now;
    }

    public void deleteFeedback(Instant now) {
        this.feedback = null;
        this.feedbackUpdatedAt = null;
        this.feedbackReadAt = null;
        this.updatedAt = now;
    }

    public void markFeedbackRead(Instant now) {
        if (feedback != null) feedbackReadAt = now;
    }

    public boolean isFeedbackUnread() {
        return feedback != null && (feedbackReadAt == null || feedbackReadAt.isBefore(feedbackUpdatedAt));
    }

    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public AdminRequestKind getKind() { return kind; }
    public String getMessage() { return message; }
    public String getFeedback() { return feedback; }
    public Instant getFeedbackUpdatedAt() { return feedbackUpdatedAt; }
    public Instant getFeedbackReadAt() { return feedbackReadAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
