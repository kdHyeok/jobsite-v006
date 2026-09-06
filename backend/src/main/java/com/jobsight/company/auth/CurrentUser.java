package com.jobsight.company.auth;

import com.jobsight.company.common.ApiRuleException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * 현재 로그인 계정을 한 곳에서만 읽는다.
 * 서비스가 직접 이 컴포넌트로 소유자 id 를 얻기 때문에
 * 컨트롤러가 소유자 전달을 빠뜨려 데이터가 섞이는 실수가 구조적으로 불가능하다.
 */
@Component
public class CurrentUser {

    public Optional<AppOidcUser> find() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        if (authentication.getPrincipal() instanceof AppOidcUser principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    public AppOidcUser require() {
        return find().orElseThrow(() -> new ApiRuleException(
                HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "로그인이 필요합니다."));
    }

    public UUID id() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof com.jobsight.company.mcp.McpPrincipal principal) {
            return principal.userId();
        }
        return require().appUserId();
    }
}
