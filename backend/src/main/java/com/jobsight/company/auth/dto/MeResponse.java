package com.jobsight.company.auth.dto;

import com.jobsight.company.user.AppUser;
import com.jobsight.company.user.UserRole;

import java.util.UUID;

/** 세션 principal 이 아니라 DB 의 현재 값으로 만든다. 이름을 바꾸면 다음 조회에 바로 반영되어야 하기 때문이다. */
public record MeResponse(boolean authenticated, UUID id, String email, String displayName, UserRole role) {

    public static MeResponse anonymous() {
        return new MeResponse(false, null, null, null, null);
    }

    public static MeResponse of(AppUser user) {
        return new MeResponse(true, user.getId(), user.getEmail(), user.getDisplayName(), user.getRole());
    }
}
