package com.jobsight.company.posting;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * 공고가 가진 채용 절차 한 단계. 공고에 완전히 종속된다(cascade, orphanRemoval).
 * 순서(seq)는 JobPosting 의 @OrderColumn 이 관리하므로 여기엔 없다.
 */
@Entity
@Table(name = "recruitment_steps")
public class RecruitmentStep {
    @Id
    private UUID id;

    @Column(nullable = false, length = 60)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StepResult result;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(columnDefinition = "TEXT")
    private String memo;

    protected RecruitmentStep() {
    }

    public RecruitmentStep(String name, StepResult result, Instant scheduledAt, String memo) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.result = result == null ? StepResult.UPCOMING : result;
        this.scheduledAt = scheduledAt;
        this.memo = memo;
    }

    public void changeResult(StepResult result) {
        this.result = result;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public StepResult getResult() { return result; }
    public Instant getScheduledAt() { return scheduledAt; }
    public String getMemo() { return memo; }
}
