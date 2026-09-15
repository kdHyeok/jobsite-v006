package com.jobsight.company.auth;

import com.jobsight.company.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "browser_refresh_tokens")
public class BrowserRefreshToken {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private AppUser owner;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_used_at", nullable = false)
    private Instant lastUsedAt;

    protected BrowserRefreshToken() {
    }

    BrowserRefreshToken(AppUser owner, String tokenHash, Instant now, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.owner = owner;
        this.tokenHash = tokenHash;
        this.createdAt = now;
        this.lastUsedAt = now;
        this.expiresAt = expiresAt;
    }

    void rotate(String tokenHash, Instant now) {
        this.tokenHash = tokenHash;
        this.lastUsedAt = now;
    }

    boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    UUID ownerId() { return owner.getId(); }
    String tokenHash() { return tokenHash; }
    Instant expiresAt() { return expiresAt; }
}
