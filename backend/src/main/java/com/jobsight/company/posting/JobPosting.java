package com.jobsight.company.posting;

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
@Table(name = "job_postings")
public class JobPosting {
    @Id
    private UUID id;

    /** 계정별 데이터 분리의 기준. 기업과 같은 규칙이다. */
    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    /** NULL 이면 아직 기업에 연결하지 않은 공고다. */
    @Column(name = "company_id")
    private UUID companyId;

    /** 기업 미연결 공고의 고용회사명. 연결되면 기업 이름을 쓴다. */
    @Column(name = "company_name_snapshot", length = 120)
    private String companyNameSnapshot;

    @Column(nullable = false, length = 160)
    private String position;

    @Column(name = "posting_url", length = 500)
    private String postingUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", nullable = false, length = 20)
    private EmploymentType employmentType;

    /** NULL 이면 상시채용. 정렬에서 맨 뒤로 간다. */
    @Column(name = "deadline_at")
    private Instant deadlineAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationStage stage;

    @Column(length = 60)
    private String headcount;

    @Column(name = "work_location", length = 160)
    private String workLocation;

    @Column(columnDefinition = "TEXT")
    private String qualifications;

    @Column(columnDefinition = "TEXT")
    private String responsibilities;

    @Column(name = "required_skills", columnDefinition = "TEXT")
    private String requiredSkills;

    /** NULL 이 아니면 보관함. */
    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected JobPosting() {
    }

    public JobPosting(UUID ownerId, JobPostingAttributes attributes) {
        this.id = UUID.randomUUID();
        this.ownerId = ownerId;
        apply(attributes);
    }

    public void update(JobPostingAttributes attributes) {
        apply(attributes);
        this.updatedAt = Instant.now();
    }

    private void apply(JobPostingAttributes attributes) {
        this.companyId = attributes.companyId();
        this.companyNameSnapshot = attributes.companyName();
        this.position = attributes.position();
        this.postingUrl = attributes.postingUrl();
        this.employmentType = attributes.employmentType();
        this.deadlineAt = attributes.deadlineAt();
        this.stage = attributes.stage();
        this.headcount = attributes.headcount();
        this.workLocation = attributes.workLocation();
        this.qualifications = attributes.qualifications();
        this.responsibilities = attributes.responsibilities();
        this.requiredSkills = attributes.requiredSkills();
    }

    public void changeStage(ApplicationStage stage) {
        this.stage = stage;
        // 지원을 시작한 공고가 보관함에 있으면 앞뒤가 맞지 않는다. 단계를 옮기면 꺼낸다.
        if (!stage.isAutoArchivable()) {
            this.archivedAt = null;
        }
        this.updatedAt = Instant.now();
    }

    public void archive(Instant at) {
        this.archivedAt = at;
        this.updatedAt = at;
    }

    public void restore() {
        this.archivedAt = null;
        this.updatedAt = Instant.now();
    }

    public boolean isArchived() {
        return archivedAt != null;
    }

    /** 마감이 지났고, 아직 지원하지 않은 관심 공고인가. */
    public boolean shouldAutoArchive(Instant now) {
        return !isArchived()
                && deadlineAt != null
                && deadlineAt.isBefore(now)
                && stage.isAutoArchivable();
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
    public UUID getCompanyId() { return companyId; }
    public String getCompanyNameSnapshot() { return companyNameSnapshot; }
    public String getPosition() { return position; }
    public String getPostingUrl() { return postingUrl; }
    public EmploymentType getEmploymentType() { return employmentType; }
    public Instant getDeadlineAt() { return deadlineAt; }
    public ApplicationStage getStage() { return stage; }
    public String getHeadcount() { return headcount; }
    public String getWorkLocation() { return workLocation; }
    public String getQualifications() { return qualifications; }
    public String getResponsibilities() { return responsibilities; }
    public String getRequiredSkills() { return requiredSkills; }
    public Instant getArchivedAt() { return archivedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
