package com.jobsight.company.auth;

import com.jobsight.company.user.AppUser;
import com.jobsight.company.user.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthControllerRefreshTest {
    @Test
    void refreshPersistsAuthenticatedPrincipalInNewSession() {
        var refreshTokens = mock(BrowserRefreshTokenService.class);
        var user = AppUser.signUpWithGoogle(
                "user@example.com", "google-sub", "사용자", UserStatus.ACTIVE);
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        when(refreshTokens.rotate(request, response)).thenReturn(Optional.of(user));

        var controller = new AuthController(null, null, null, refreshTokens, null);
        assertThat(controller.refresh(request, response).getStatusCode().value()).isEqualTo(204);

        SecurityContext context = (SecurityContext) request.getSession().getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertThat(context.getAuthentication().isAuthenticated()).isTrue();
        assertThat(context.getAuthentication().getPrincipal()).isEqualTo(new AppSessionUser(user));
    }
}
