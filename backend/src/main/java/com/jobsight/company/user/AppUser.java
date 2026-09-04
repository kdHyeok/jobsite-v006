package com.jobsight.company.user;

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
@Table(name = "app_users")
public class AppUser {
    /**
     * V4 마이그레이션이 삽입하는 고정 행. 시드 기업 데이터의 소유자다.
     * 아직 아무 Google 계정도 연결되지 않았다면, 관리자 이메일의 첫 Google 로그인이 이 행을 넘겨받아
     * 시드 데이터 소유권을 이어받는다.
     */
    public static final UUID BOOTSTRAP_ADMIN_ID =
            UUID.fromString("00000000-0000-0000-0000-0000000000ad");

    @Id
    private UUID id;

    @Column(nullable = false, length = 190)
    private String email;

    /** Google 의 안정적인 식별자. 이메일이 바뀌어도 이 값으로 계정을 찾는다. NULL 이면 로그인할 수 없다. */
    @Column(name = "google_sub", length = 255)
    private String googleSub;

    /** 사용자가 바꿀 수 있는 표시 이름. 첫 로그인 때 Google name 클레임으로 채워진다. */
    @Column(name = "display_name", length = 80)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AppUser() {
    }

    private AppUser(String email, String googleSub, String displayName, UserRole role, UserStatus status) {
        this.id = UUID.randomUUID();
        this.email = email;
        this.googleSub = googleSub;
        this.displayName = displayName;
        this.role = role;
        this.status = status;
    }

    /** Google 최초 로그인으로 만들어지는 계정. 자동 승인 설정에 따라 ACTIVE 또는 PENDING 으로 시작한다. */
    public static AppUser signUpWithGoogle(String email, String googleSub, String displayName, UserStatus initialStatus) {
        return new AppUser(email, googleSub, displayName, UserRole.USER, initialStatus);
    }

    public void changeStatus(UserStatus status) {
        this.status = status;
    }

    public void changeRole(UserRole role) {
        this.role = role;
    }

    /** null 이면 이름을 지운다. 정규화(trim, 빈 문자열 → null)는 서비스가 한다. */
    public void changeDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * 기존 계정에 Google 신원을 연결한다.
     * 이메일이 Google 에서 검증된 경우에만 호출해야 한다(미검증 이메일 연결은 계정 탈취가 된다).
     * 표시 이름은 비어 있을 때만 Google 값으로 채운다 — 사용자가 정한 이름을 덮어쓰지 않는다.
     */
    public void linkGoogle(String googleSub, String verifiedEmail, String googleName) {
        this.googleSub = googleSub;
        this.email = verifiedEmail;
        if (this.displayName == null) {
            this.displayName = googleName;
        }
    }

    /** 설정된 관리자 이메일은 항상 활성 관리자여야 한다. */
    public void promoteToAdmin() {
        this.role = UserRole.ADMIN;
        this.status = UserStatus.ACTIVE;
    }

    public boolean isGoogleLinked() {
        return googleSub != null;
    }

    public boolean isActiveAdmin() {
        return role == UserRole.ADMIN && status == UserStatus.ACTIVE;
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
    public String getEmail() { return email; }
    public String getGoogleSub() { return googleSub; }
    public String getDisplayName() { return displayName; }
    public UserRole getRole() { return role; }
    public UserStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
