package com.jobsight.company.auth;

import com.jobsight.company.user.AppUser;
import com.jobsight.company.user.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 이 애플리케이션의 유일한 principal. Google 로그인만 존재한다.
 * OIDC 관련 값은 Spring 이 만들어 준 OidcUser 에 위임하고,
 * 권한과 애플리케이션 식별자만 우리 DB 의 계정에서 가져온다.
 */
public class AppOidcUser implements OidcUser {
    private final OidcUser delegate;
    private final UUID id;
    private final String email;
    private final UserRole role;

    public AppOidcUser(OidcUser delegate, AppUser user) {
        this.delegate = delegate;
        this.id = user.getId();
        this.email = user.getEmail();
        this.role = user.getRole();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public Map<String, Object> getAttributes() {
        return delegate.getAttributes();
    }

    @Override
    public Map<String, Object> getClaims() {
        return delegate.getClaims();
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return delegate.getUserInfo();
    }

    @Override
    public OidcIdToken getIdToken() {
        return delegate.getIdToken();
    }

    /** 세션에 남는 이름은 Google sub 대신 우리 계정 id 로 고정한다. */
    @Override
    public String getName() {
        return id.toString();
    }

    public UUID appUserId() {
        return id;
    }

    public String appEmail() {
        return email;
    }

    public UserRole appRole() {
        return role;
    }
}
