package com.jobsight.company.user.dto;

import com.jobsight.company.user.AppUser;
import com.jobsight.company.user.UserRole;
import com.jobsight.company.user.UserStatus;

import java.time.Instant;
import java.util.UUID;

/** 관리자 화면용 계정 표현. Google sub 원값은 포함하지 않는다. */
public record UserResponse(
        UUID id,
        String email,
        String displayName,
        UserRole role,
        UserStatus status,
        /** false 면 로그인 수단이 없는 행이다(비밀번호 로그인 제거 이전에 만들어진 계정). */
        boolean googleLinked,
        Instant createdAt,
        Instant updatedAt
) {
    public static UserResponse from(AppUser user) {
        return new UserResponse(
                user.getId(), user.getEmail(), user.getDisplayName(), user.getRole(), user.getStatus(),
                user.isGoogleLinked(), user.getCreatedAt(), user.getUpdatedAt()
        );
    }
}
