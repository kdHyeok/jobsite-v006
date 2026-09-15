package com.jobsight.company.adminrequest;

import com.jobsight.company.adminrequest.dto.*;
import com.jobsight.company.auth.CurrentUser;
import com.jobsight.company.common.ApiRuleException;
import com.jobsight.company.common.ResourceNotFoundException;
import com.jobsight.company.user.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminRequestServiceTest {
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Instant NOW = Instant.parse("2026-09-15T03:00:00Z");

    @Mock AdminRequestRepository requests;
    @Mock AppUserRepository users;
    @Mock CurrentUser currentUser;
    private AdminRequestService service;
    private AppUser owner;

    @BeforeEach
    void setUp() {
        service = new AdminRequestService(requests, users, currentUser, Clock.fixed(NOW, ZoneOffset.UTC));
        owner = AppUser.signUpWithGoogle("user@example.com", "sub", "사용자", UserStatus.ACTIVE);
        given(currentUser.id()).willReturn(OWNER);
    }

    @Test
    void createLocksAccountAndRejectsRequestsWithinFiveSeconds() {
        given(users.findByIdForUpdate(OWNER)).willReturn(Optional.of(owner));
        given(requests.save(any(AdminRequest.class))).willAnswer(call -> call.getArgument(0));
        var input = new AdminRequestWriteRequest(AdminRequestKind.BUG_REPORT, "  화면이 열리지 않습니다.  ");

        var created = service.create(input);

        assertThat(created.message()).isEqualTo("화면이 열리지 않습니다.");
        assertThat(owner.getAdminRequestCount()).isEqualTo(1);
        assertThatThrownBy(() -> service.create(input))
                .isInstanceOfSatisfying(ApiRuleException.class,
                        error -> assertThat(error.getCode()).isEqualTo("REQUEST_TOO_FAST"));
    }

    @Test
    void dailyLimitSurvivesRequestDeletionBecauseQuotaLivesOnAccount() {
        LocalDate today = LocalDate.ofInstant(NOW, ZoneId.of("Asia/Seoul"));
        for (int i = 0; i < 50; i++) owner.recordAdminRequest(today, NOW.minusSeconds(6));
        given(users.findByIdForUpdate(OWNER)).willReturn(Optional.of(owner));

        assertThatThrownBy(() -> service.create(
                new AdminRequestWriteRequest(AdminRequestKind.FEATURE_REQUEST, "새 기능을 제안합니다.")))
                .isInstanceOfSatisfying(ApiRuleException.class,
                        error -> assertThat(error.getCode()).isEqualTo("REQUEST_DAILY_LIMIT"));
    }

    @Test
    void userMutationUsesOwnerFilteredLookup() {
        UUID id = UUID.randomUUID();
        given(requests.findByIdAndOwnerId(id, OWNER)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id,
                new AdminRequestWriteRequest(AdminRequestKind.BUG_REPORT, "타인 요청 수정 시도")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void adminFeedbackBecomesUnreadUntilOwnerOpensIt() {
        var request = new AdminRequest(OWNER, AdminRequestKind.BUG_REPORT, "오류가 반복됩니다.", NOW.minusSeconds(30));
        given(requests.save(any(AdminRequest.class))).willAnswer(call -> call.getArgument(0));
        given(requests.findById(request.getId())).willReturn(Optional.of(request));
        given(users.findById(OWNER)).willReturn(Optional.of(owner));
        given(requests.findByIdAndOwnerId(request.getId(), OWNER)).willReturn(Optional.of(request));

        var replied = service.saveFeedback(request.getId(), new AdminFeedbackRequest("수정 사항을 확인해 주세요."));
        assertThat(replied.feedbackUnread()).isTrue();

        var read = service.readFeedback(request.getId());
        assertThat(read.feedbackUnread()).isFalse();
        verify(requests).findByIdAndOwnerId(request.getId(), OWNER);
    }
}
