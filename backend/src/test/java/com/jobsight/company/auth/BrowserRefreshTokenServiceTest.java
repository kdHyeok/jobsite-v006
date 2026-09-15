package com.jobsight.company.auth;

import com.jobsight.company.user.AppUser;
import com.jobsight.company.user.AppUserRepository;
import com.jobsight.company.user.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BrowserRefreshTokenServiceTest {
    @Test
    void rotatesOpaqueTokenWithoutExtendingAbsoluteExpiry() {
        var tokens = mock(BrowserRefreshTokenRepository.class);
        var users = mock(AppUserRepository.class);
        var clock = Clock.fixed(Instant.parse("2026-09-16T00:00:00Z"), ZoneOffset.UTC);
        var service = new BrowserRefreshTokenService(tokens, users, false, clock, new SecureRandom());
        var user = AppUser.signUpWithGoogle(
                "user@example.com", "google-sub", "사용자", UserStatus.ACTIVE);
        when(tokens.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(users.findById(user.getId())).thenReturn(Optional.of(user));

        var loginResponse = new MockHttpServletResponse();
        service.issue(loginResponse, user.getId());

        var issued = loginResponse.getCookie(BrowserRefreshTokenService.COOKIE_NAME);
        var stored = org.mockito.ArgumentCaptor.forClass(BrowserRefreshToken.class);
        org.mockito.Mockito.verify(tokens).save(stored.capture());
        String firstHash = stored.getValue().tokenHash();
        Instant firstExpiry = stored.getValue().expiresAt();
        when(tokens.findByTokenHashForUpdate(firstHash)).thenReturn(Optional.of(stored.getValue()));

        var refreshRequest = new MockHttpServletRequest();
        refreshRequest.setCookies(issued);
        var refreshResponse = new MockHttpServletResponse();

        assertThat(service.rotate(refreshRequest, refreshResponse)).contains(user);
        assertThat(stored.getValue().tokenHash()).isNotEqualTo(firstHash);
        assertThat(stored.getValue().expiresAt()).isEqualTo(firstExpiry);
        assertThat(refreshResponse.getCookie(BrowserRefreshTokenService.COOKIE_NAME).getValue())
                .isNotEqualTo(issued.getValue());
    }
}
