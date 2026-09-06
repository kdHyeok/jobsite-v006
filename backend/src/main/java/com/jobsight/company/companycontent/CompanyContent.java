package com.jobsight.company.companycontent;

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

@Entity
@Table(name = "company_contents")
public class CompanyContent {
    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CompanyContentKind kind;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String preview;

    @Column(length = 160)
    private String source;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CompanyContent() {
    }

    public CompanyContent(UUID ownerId, UUID companyId, CompanyContentKind kind,
                          String title, String preview, String source, String url) {
        this.id = UUID.randomUUID();
        this.ownerId = ownerId;
        this.companyId = companyId;
        apply(kind, title, preview, source, url);
    }

    public void update(CompanyContentKind kind, String title, String preview, String source, String url) {
        apply(kind, title, preview, source, url);
    }

    private void apply(CompanyContentKind kind, String title, String preview, String source, String url) {
        this.kind = kind;
        this.title = title;
        this.preview = preview;
        this.source = source;
        this.url = url;
    }

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public UUID getCompanyId() { return companyId; }
    public CompanyContentKind getKind() { return kind; }
    public String getTitle() { return title; }
    public String getPreview() { return preview; }
    public String getSource() { return source; }
    public String getUrl() { return url; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
