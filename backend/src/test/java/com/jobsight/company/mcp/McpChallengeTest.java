package com.jobsight.company.mcp;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 401 의 WWW-Authenticate scope 힌트를 고정한다.
 * 배포에서 일반 사용자가 읽기 권한만 받던 원인이 여기 read 만 적혀 있던 것이었다.
 */
class McpChallengeTest {

    private final McpOAuthConfig config = new McpOAuthConfig("https://example.com");

    @Test
    void challengeAdvertisesWriteSoClientsRequestIt() {
        assertThat(config.challenge())
                .contains("scope=\"" + McpOAuthConfig.READ + " " + McpOAuthConfig.WRITE + "\"")
                .contains("resource_metadata=\"https://example.com/.well-known/oauth-protected-resource\"");
    }

    /** admin 은 계정 역할까지 필요하다. 모든 사용자 동의 화면에 올리지 않는다. */
    @Test
    void challengeLeavesAdminOutOfTheDefaultHint() {
        assertThat(config.challenge()).doesNotContain(McpOAuthConfig.ADMIN);
        assertThat(McpOAuthConfig.SCOPES).contains(McpOAuthConfig.ADMIN);
    }
}
