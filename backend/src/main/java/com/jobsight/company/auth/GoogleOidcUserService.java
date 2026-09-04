package com.jobsight.company.auth;

import com.jobsight.company.common.ApiRuleException;
import com.jobsight.company.user.AppUser;
import com.jobsight.company.user.AppUserService;
import com.jobsight.company.user.UserStatus;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

/**
 * Google 로그인의 유일한 관문.
 * Google 이 준 신원을 우리 계정으로 해석하고, 계정 상태로 로그인 가능 여부를 판정한다.
 */
@Service
public class GoogleOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {
    private final OidcUserService delegate = new OidcUserService();
    private final AppUserService users;

    public GoogleOidcUserService(AppUserService users) {
        this.users = users;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest request) throws OAuth2AuthenticationException {
        OidcUser googleUser = delegate.loadUser(request);

        String subject = googleUser.getSubject();
        if (subject == null || subject.isBlank()) {
            throw error("INVALID_IDENTITY", "Google 계정 식별자를 확인할 수 없습니다.");
        }

        AppUser user;
        try {
            // 이 호출의 트랜잭션이 먼저 커밋되므로, 아래 상태 검사에서 예외가 나도
            // 방금 만들어진 가입 신청 계정은 유지된다.
            user = users.resolveGoogleUser(
                    subject,
                    googleUser.getEmail(),
                    Boolean.TRUE.equals(googleUser.getEmailVerified()),
                    googleUser.getFullName()
            );
        } catch (ApiRuleException exception) {
            throw error(exception.getCode(), exception.getMessage());
        }

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw error("ACCOUNT_SUSPENDED", "정지된 계정입니다. 관리자에게 문의하세요.");
        }
        if (!user.getStatus().canLogIn()) {
            throw error("ACCOUNT_NOT_APPROVED",
                    "가입 신청이 접수되었습니다. 관리자 승인 후 로그인할 수 있습니다.");
        }
        return new AppOidcUser(googleUser, user);
    }

    private OAuth2AuthenticationException error(String code, String message) {
        return new OAuth2AuthenticationException(new OAuth2Error(code, message, null), message);
    }
}
