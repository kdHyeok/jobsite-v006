package com.jobsight.company.posting;

import com.jobsight.company.auth.CurrentUser;
import com.jobsight.company.common.ResourceNotFoundException;
import com.jobsight.company.company.CompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JobPostingServiceTest {
    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock
    private JobPostingRepository repository;
    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private CurrentUser currentUser;

    private JobPostingService service;

    @BeforeEach
    void setUp() {
        service = new JobPostingService(repository, companyRepository, currentUser);
    }

    private static JobPosting posting(Instant deadline, ApplicationStage stage) {
        JobPosting created = new JobPosting(OWNER_ID, new JobPostingAttributes(
                null, "테스트 회사", "백엔드 개발", null,
                EmploymentType.FULL_TIME, deadline, stage,
                null, null, null, null, null));
        created.onCreate();
        return created;
    }

    /** 마감이 지난 '관심' 공고는 목록 조회 시점에 보관함으로 옮겨진다. */
    @Test
    void expiredInterestedPostingIsArchivedOnList() {
        Instant past = Instant.now().minus(1, ChronoUnit.DAYS);
        JobPosting expired = posting(past, ApplicationStage.INTERESTED);
        given(currentUser.id()).willReturn(OWNER_ID);
        given(repository.findExpiredInterested(any(), any())).willReturn(List.of(expired));
        given(repository.findOpen(OWNER_ID)).willReturn(List.of());

        service.findOpen();

        assertThat(expired.isArchived()).isTrue();
        verify(repository).saveAll(anyList());
    }

    /** 지원한 공고는 마감이 지나도 결과를 기다리는 중이라 보관하지 않는다. */
    @Test
    void submittedPostingIsNotAutoArchivable() {
        Instant past = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant now = Instant.now();

        assertThat(posting(past, ApplicationStage.SUBMITTED).shouldAutoArchive(now)).isFalse();
        assertThat(posting(past, ApplicationStage.INTERVIEW).shouldAutoArchive(now)).isFalse();
        assertThat(posting(past, ApplicationStage.INTERESTED).shouldAutoArchive(now)).isTrue();
    }

    /** 마감이 남았으면 관심 공고여도 보관하지 않는다. 상시채용(마감 없음)도 마찬가지. */
    @Test
    void futureOrOpenEndedPostingIsNotArchived() {
        Instant now = Instant.now();
        Instant future = now.plus(1, ChronoUnit.DAYS);

        assertThat(posting(future, ApplicationStage.INTERESTED).shouldAutoArchive(now)).isFalse();
        assertThat(posting(null, ApplicationStage.INTERESTED).shouldAutoArchive(now)).isFalse();
    }

    /** 관심 외 단계로 옮기면 보관함에서 꺼낸다. 지원 중인 공고가 보관함에 있으면 앞뒤가 맞지 않는다. */
    @Test
    void movingStageOutOfInterestedRestoresFromArchive() {
        JobPosting archived = posting(Instant.now().minus(1, ChronoUnit.DAYS), ApplicationStage.INTERESTED);
        archived.archive(Instant.now());
        assertThat(archived.isArchived()).isTrue();

        archived.changeStage(ApplicationStage.SUBMITTED);

        assertThat(archived.isArchived()).isFalse();
    }

    /** 다른 계정 소유의 공고는 존재를 알리지 않고 404. */
    @Test
    void otherOwnersPostingIsNotReachable() {
        UUID id = UUID.randomUUID();
        given(currentUser.id()).willReturn(OWNER_ID);
        given(repository.findByIdAndOwnerId(id, OWNER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(ResourceNotFoundException.class);
    }
}
