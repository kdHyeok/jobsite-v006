package com.jobsight.company.auth;

import java.security.Principal;
import java.util.UUID;

/** Google 로그인 세션과 갱신된 세션이 공유하는 애플리케이션 신원. */
public interface AppPrincipal extends Principal {
    UUID appUserId();
}
