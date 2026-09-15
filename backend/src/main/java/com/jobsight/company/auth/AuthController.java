package com.jobsight.company.auth;

import com.jobsight.company.auth.dto.MeResponse;
import com.jobsight.company.common.ApiRuleException;
import com.jobsight.company.common.ApiPaths;
import com.jobsight.company.setting.AppSettingService;
import com.jobsight.company.user.AppUserService;
import com.jobsight.company.user.dto.DisplayNameUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;

/**
 * 로그인은 Google OAuth 로만 한다: GET /oauth2/authorization/google (Spring Security 필터).
 * 로그아웃은 POST /api/auth/logout (Spring Security 필터).
 * 여기에는 세션 조회·갱신, 본인 이름 수정, 로그인 화면 옵션이 있다.
 */
@Tag(name = "auth", description = "세션 상태·갱신, 본인 정보와 로그인 화면 옵션. Google 로그인과 로그아웃은 Spring Security 필터가 처리한다.")
@RestController
@RequestMapping(ApiPaths.AUTH)
public class AuthController {
    private final AppUserService users;
    private final AppSettingService settings;
    private final CurrentUser currentUser;
    private final BrowserRefreshTokenService refreshTokens;
    private final ObjectProvider<ClientRegistrationRepository> clientRegistrations;
    private final SecurityContextRepository securityContexts = new HttpSessionSecurityContextRepository();

    public AuthController(AppUserService users, AppSettingService settings, CurrentUser currentUser,
                          BrowserRefreshTokenService refreshTokens,
                          ObjectProvider<ClientRegistrationRepository> clientRegistrations) {
        this.users = users;
        this.settings = settings;
        this.currentUser = currentUser;
        this.refreshTokens = refreshTokens;
        this.clientRegistrations = clientRegistrations;
    }

    @Operation(summary = "현재 세션의 계정 정보",
            description = "비로그인이면 authenticated=false 로 200. DB 의 현재 값을 반환하므로 이름 변경이 바로 반영된다.")
    @GetMapping(ApiPaths.AUTH_ME)
    public MeResponse me() {
        return currentUser.find()
                .flatMap(principal -> users.findById(principal.appUserId()))
                .map(MeResponse::of)
                .orElseGet(MeResponse::anonymous);
    }

    @Operation(summary = "본인 표시 이름 변경", description = "빈 값이면 이름을 지운다. 이메일은 Google 신원이라 바꿀 수 없다.")
    @PatchMapping(ApiPaths.AUTH_ME)
    public MeResponse updateMe(@Valid @RequestBody DisplayNameUpdateRequest request) {
        return MeResponse.of(users.changeDisplayName(currentUser.id(), request.displayName()));
    }

    @Operation(summary = "로그인 화면 옵션",
            description = "Google 자격증명 설정 여부와 신규 가입 자동 승인 여부. 공개 엔드포인트.")
    @GetMapping(ApiPaths.AUTH_LOGIN_OPTIONS)
    public Map<String, Boolean> loginOptions() {
        return Map.of(
                "googleEnabled", clientRegistrations.getIfAvailable() != null,
                "autoApproveSignup", settings.isAutoApproveSignup()
        );
    }

    @Operation(summary = "브라우저 세션 갱신",
            description = "HttpOnly 리프레시 토큰을 회전하고 새 30분 세션을 만든다. 토큰이 없거나 계정이 비활성이면 401.")
    @PostMapping(ApiPaths.AUTH_REFRESH)
    public ResponseEntity<Void> refresh(HttpServletRequest request, HttpServletResponse response) {
        var user = refreshTokens.rotate(request, response)
                .orElseThrow(() -> new ApiRuleException(
                        HttpStatus.UNAUTHORIZED,
                        "REFRESH_REJECTED", "로그인을 다시 진행해 주세요."));

        AppSessionUser principal = new AppSessionUser(user);
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities());
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        request.getSession(true);
        request.changeSessionId();
        securityContexts.saveContext(context, request, response);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "nginx auth_request 전용 관리자 확인",
            description = "SecurityConfig 가 ADMIN 을 요구하므로 도달하면 204. /admin 페이지 게이트가 호출한다.")
    @GetMapping(ApiPaths.AUTH_ADMIN_CHECK)
    public ResponseEntity<Void> adminCheck() {
        return ResponseEntity.noContent().build();
    }
}
