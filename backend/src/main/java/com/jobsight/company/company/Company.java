package com.jobsight.company.company;

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
@Table(name = "companies")
public class Company {
    @Id
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 120)
    private String industry;

    @Column(length = 160)
    private String location;

    @Column(name = "website_url", length = 500)
    private String websiteUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CompanyStatus status;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Company() {
    }

    public Company(String name, String industry, String location, String websiteUrl,
                   CompanyStatus status, String summary, String memo) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.industry = industry;
        this.location = location;
        this.websiteUrl = websiteUrl;
        this.status = status;
        this.summary = summary;
        this.memo = memo;
    }

    public void update(String name, String industry, String location, String websiteUrl,
                       CompanyStatus status, String summary, String memo) {
        this.name = name;
        this.industry = industry;
        this.location = location;
        this.websiteUrl = websiteUrl;
        this.status = status;
        this.summary = summary;
        this.memo = memo;
        this.updatedAt = Instant.now();
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
    public String getName() { return name; }
    public String getIndustry() { return industry; }
    public String getLocation() { return location; }
    public String getWebsiteUrl() { return websiteUrl; }
    public CompanyStatus getStatus() { return status; }
    public String getSummary() { return summary; }
    public String getMemo() { return memo; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
