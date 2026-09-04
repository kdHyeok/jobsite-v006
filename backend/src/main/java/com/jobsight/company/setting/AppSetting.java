package com.jobsight.company.setting;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

/** 애플리케이션 전역 설정. id = 1 인 행 하나만 존재한다(DB CHECK 제약으로 강제). */
@Entity
@Table(name = "app_settings")
public class AppSetting {
    public static final short SINGLETON_ID = 1;

    @Id
    private Short id;

    /**
     * true: Google 첫 로그인이 곧 가입 완료(ACTIVE).
     * false: 가입 신청(PENDING)으로 접수되고 관리자 승인 후 ACTIVE.
     * 가입을 막는 상태는 없다 — 승인 대기 목록이 곧 대기열이다.
     */
    @Column(name = "auto_approve_signup", nullable = false)
    private boolean autoApproveSignup;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AppSetting() {
    }

    public void changeAutoApproveSignup(boolean autoApproveSignup) {
        this.autoApproveSignup = autoApproveSignup;
        // @PreUpdate 는 커밋 시점에 실행되어 이미 만들어진 응답 DTO 에 반영되지 않는다.
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Short getId() { return id; }
    public boolean isAutoApproveSignup() { return autoApproveSignup; }
    public Instant getUpdatedAt() { return updatedAt; }
}
