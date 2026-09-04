package com.jobsight.company.auth;

import com.jobsight.company.common.ApiError;
import com.jobsight.company.common.ApiPaths;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 인증 수단은 Google OIDC 하나다. 비밀번호 로그인은 존재하지 않는다.
 * 경로는 모두 ApiPaths 상수를 참조한다. 여기서 문자열 리터럴로 경로를 쓰지 않는다.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final ObjectMapper objectMapper;

    public SecurityConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            GoogleOidcUserService googleOidcUserService,
            ObjectProvider<ClientRegistrationRepository> clientRegistrations) throws Exception {

        // 쿠키 기반 세션 인증을 쓰는 순간 CSRF가 실제 공격 벡터가 되므로 토큰을 유지한다.
        // SPA가 읽어야 하므로 HttpOnly는 끄고, 세션 쿠키와 같은 Lax 로 맞춘다.
        // (Strict 는 Google 콜백처럼 cross-site 네비게이션으로 돌아오는 흐름을 깨뜨린다.)
        CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfTokenRepository.setCookieCustomizer(cookie -> cookie.sameSite("Lax"));

        // 기본 XorCsrfTokenRequestAttributeHandler는 BREACH 방어를 위해 마스킹된 토큰을 기대한다.
        // 쿠키로 원본 토큰을 내려주고 SPA가 그대로 되돌려보내는 방식에서는 평문 핸들러를 써야 한다.
        CsrfTokenRequestAttributeHandler csrfRequestHandler = new CsrfTokenRequestAttributeHandler();

        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(csrfRequestHandler))
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(ApiPaths.ACTUATOR_HEALTH).permitAll()
                        // Google 로그인 시작(/oauth2/authorization/**)과 콜백(/login/oauth2/code/**)
                        .requestMatchers("/oauth2/**", "/login/**").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                ApiPaths.AUTH + ApiPaths.AUTH_ME,
                                ApiPaths.AUTH + ApiPaths.AUTH_LOGIN_OPTIONS).permitAll()
                        .requestMatchers(ApiPaths.ADMIN + "/**", ApiPaths.AUTH + ApiPaths.AUTH_ADMIN_CHECK)
                                .hasRole("ADMIN")
                        // API 문서는 정보 노출이므로 관리자에게만 보인다.
                        .requestMatchers(ApiPaths.SWAGGER).hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .logout(logout -> logout
                        .logoutUrl(ApiPaths.AUTH_LOGOUT)
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(HttpStatus.NO_CONTENT.value()))
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(this::onUnauthenticated)
                        .accessDeniedHandler(this::onAccessDenied)
                );

        // 자격증명이 설정되지 않으면 ClientRegistrationRepository 빈이 없다.
        // 그때는 oauth2Login 을 붙이지 않아 앱이 정상 기동하고, 로그인 수단이 없는 상태가 된다.
        if (clientRegistrations.getIfAvailable() != null) {
            http.oauth2Login(oauth -> oauth
                    .userInfoEndpoint(userInfo -> userInfo.oidcUserService(googleOidcUserService))
                    .defaultSuccessUrl("/", true)
                    .failureHandler(this::onOAuthFailure)
            );
        }

        return http.build();
    }

    /**
     * Google 로그인은 브라우저 리다이렉트 흐름이므로 JSON 대신 쿼리 파라미터로 결과를 전달한다.
     * SPA가 /?authError=CODE 를 읽어 안내 문구를 띄운다.
     */
    private void onOAuthFailure(HttpServletRequest request, HttpServletResponse response,
                                AuthenticationException exception) throws IOException {
        String code = "OAUTH_FAILED";
        if (exception instanceof OAuth2AuthenticationException oauthException) {
            code = oauthException.getError().getErrorCode();
            // 코드만 리다이렉트로 넘기면 원인을 잃는다. 진단용으로 설명까지 남긴다. 토큰은 기록하지 않는다.
            log.warn("Google 로그인 실패 [{}]: {}", code, oauthException.getError().getDescription());
        } else {
            log.warn("Google 로그인 실패: {}", exception.getMessage());
        }
        response.sendRedirect("/?authError=" + URLEncoder.encode(code, StandardCharsets.UTF_8));
    }

    private void onUnauthenticated(HttpServletRequest request, HttpServletResponse response,
                                   AuthenticationException exception) throws IOException {
        writeJson(response, HttpStatus.UNAUTHORIZED, ApiError.of(
                HttpStatus.UNAUTHORIZED.value(), "UNAUTHENTICATED", "로그인이 필요합니다."));
    }

    private void onAccessDenied(HttpServletRequest request, HttpServletResponse response,
                                AccessDeniedException exception) throws IOException {
        writeJson(response, HttpStatus.FORBIDDEN, ApiError.of(
                HttpStatus.FORBIDDEN.value(), "FORBIDDEN", "권한이 없습니다."));
    }

    private void writeJson(HttpServletResponse response, HttpStatus status, Object body) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), body);
    }

    /**
     * Spring Security는 CSRF 토큰을 지연 생성하므로, 아무도 토큰을 읽지 않으면
     * XSRF-TOKEN 쿠키가 내려가지 않는다. 매 요청에서 토큰을 실제로 만들어 쿠키를 보장한다.
     */
    private static final class CsrfCookieFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {
            CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (token != null) {
                token.getToken();
            }
            filterChain.doFilter(request, response);
        }
    }
}
