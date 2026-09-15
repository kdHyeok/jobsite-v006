package com.jobsight.company.auth;

import com.jobsight.company.user.AppUser;
import com.jobsight.company.user.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** 리프레시 토큰으로 새로 만든 세션에는 Google 클레임 대신 앱 신원만 저장한다. */
public record AppSessionUser(UUID appUserId, String appEmail, UserRole appRole)
        implements AppPrincipal, Serializable {

    public AppSessionUser(AppUser user) {
        this(user.getId(), user.getEmail(), user.getRole());
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + appRole.name()));
    }

    @Override
    public String getName() {
        return appUserId.toString();
    }
}
