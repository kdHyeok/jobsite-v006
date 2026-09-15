package com.jobsight.company.selfintro;

import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.JoinColumn;

import java.time.Instant;
import java.util.UUID;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "self_introductions")
public class SelfIntroduction {
    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @ElementCollection
    @CollectionTable(name = "self_introduction_resumes",
            joinColumns = @JoinColumn(name = "self_introduction_id"))
    @Column(name = "resume_id", nullable = false)
    private Set<UUID> resumeIds = new LinkedHashSet<>();

    @Column(nullable = false, length = 1000)
    private String question;

    @Column(columnDefinition = "TEXT")
    private String answer;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SelfIntroduction() {
    }

    public SelfIntroduction(UUID ownerId, Set<UUID> resumeIds, String question, String answer) {
        this.id = UUID.randomUUID();
        this.ownerId = ownerId;
        update(resumeIds, question, answer);
    }

    public void update(Set<UUID> resumeIds, String question, String answer) {
        this.resumeIds.clear();
        this.resumeIds.addAll(resumeIds);
        this.question = question;
        this.answer = answer;
        this.updatedAt = Instant.now();
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public Set<UUID> getResumeIds() { return Set.copyOf(resumeIds); }
    public String getQuestion() { return question; }
    public String getAnswer() { return answer; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
