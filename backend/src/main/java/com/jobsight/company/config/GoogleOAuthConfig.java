package com.jobsight.company.config;

import com.jobsight.company.common.ApiPaths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

/**
 * Google 자격증명이 실제로 설정된 경우에만 클라이언트를 등록한다.
 *
 * Boot 의 spring.security.oauth2.client.registration.* 프로퍼티를 쓰면
 * client-id 가 빈 문자열일 때 기동 자체가 실패한다. 그래서 등록을 직접 만들고
 * 조건을 걸어, 자격증명이 없으면 앱은 그대로 뜨고 Google 로그인만 비활성되게 한다.
 *
 * redirect_uri 는 PUBLIC_BASE_URL 로 명시적으로 조립한다.
 * X-Forwarded-* 헤더에서 추론하던 방식은 nginx 내부 포트(80)와 외부 포트(8088/8443)가 달라
 * 세 번이나 어긋났다. 배포 환경이 바뀌면 .env 한 줄만 바꾼다.
 */
@Configuration
@ConditionalOnExpression(
        "!'${app.google.client-id:}'.isBlank() and !'${app.google.client-secret:}'.isBlank()")
public class GoogleOAuthConfig {

    @Bean
    ClientRegistrationRepository clientRegistrationRepository(
            @Value("${app.google.client-id}") String clientId,
            @Value("${app.google.client-secret}") String clientSecret,
            @Value("${app.public-base-url}") String publicBaseUrl) {
        String base = publicBaseUrl.trim().replaceAll("/+$", "");
        return new InMemoryClientRegistrationRepository(
                CommonOAuth2Provider.GOOGLE.getBuilder("google")
                        .clientId(clientId.trim())
                        .clientSecret(clientSecret.trim())
                        .redirectUri(base + ApiPaths.OAUTH_CALLBACK)
                        .build()
        );
    }
}
