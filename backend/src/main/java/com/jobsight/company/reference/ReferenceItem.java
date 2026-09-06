package com.jobsight.company.reference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** 참고 정보. 계정 안에서 공유되어 여러 직무에 붙는다(결정 1). */
@Entity
@Table(name = "reference_items")
public class ReferenceItem {
    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReferenceKind kind;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 500)
    private String url;

    @Column(columnDefinition = "TEXT")
    private String memo;

    /** kind=RELATED_POSITION 일 때 가리키는 내부 직무. 그 직무가 지워지면 NULL. */
    @Column(name = "related_position_id")
    private UUID relatedPositionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ReferenceItem() {
    }

    public ReferenceItem(UUID ownerId, ReferenceKind kind, String title, String url, String memo, UUID relatedPositionId) {
        this.id = UUID.randomUUID();
        this.ownerId = ownerId;
        apply(kind, title, url, memo, relatedPositionId);
    }

    public void update(ReferenceKind kind, String title, String url, String memo, UUID relatedPositionId) {
        apply(kind, title, url, memo, relatedPositionId);
        this.updatedAt = Instant.now();
    }

    private void apply(ReferenceKind kind, String title, String url, String memo, UUID relatedPositionId) {
        this.kind = kind;
        this.title = title;
        this.url = url;
        this.memo = memo;
        this.relatedPositionId = kind == ReferenceKind.RELATED_POSITION ? relatedPositionId : null;
    }

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public ReferenceKind getKind() { return kind; }
    public String getTitle() { return title; }
    public String getUrl() { return url; }
    public String getMemo() { return memo; }
    public UUID getRelatedPositionId() { return relatedPositionId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
