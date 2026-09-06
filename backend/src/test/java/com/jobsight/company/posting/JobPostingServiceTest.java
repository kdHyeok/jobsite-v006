package com.jobsight.company.posting;

import com.jobsight.company.auth.CurrentUser;
import com.jobsight.company.common.ResourceNotFoundException;
import com.jobsight.company.company.Company;
import com.jobsight.company.company.CompanyAttributes;
import com.jobsight.company.company.CompanyRepository;
import com.jobsight.company.position.Position;
import com.jobsight.company.position.PositionRepository;
import com.jobsight.company.posting.dto.JobPostingRequest;
import com.jobsight.company.posting.dto.PositionNameRequest;
import com.jobsight.company.posting.dto.StepRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JobPostingServiceTest {
    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID COMPANY_ID = UUID.fromString("00000000-0000-0000-0000-0000000000c0");

    @Mock private JobPostingRepository repository;
    @Mock private CompanyRepository companyRepository;
    @Mock private PositionRepository positionRepository;
    @Mock private CurrentUser currentUser;

    private JobPostingService service;

    @BeforeEach
    void setUp() {
        service = new JobPostingService(repository, companyRepository, positionRepository, currentUser);
    }

    private static JobPosting posting(Instant deadline, ApplicationStatus status) {
        JobPosting created = new JobPosting(OWNER_ID, new JobPostingAttributes(
                COMPANY_ID, "백엔드 개발", null, EmploymentType.FULL_TIME, deadline, status, null, List.of()));
        created.onCreate();
        return created;
    }

    private static JobPostingRequest request(UUID companyId, String companyName,
                                             List<PositionNameRequest> positions, List<StepRequest> steps) {
        return new JobPostingRequest(companyId, companyName, "2026 신입 공채", null,
                EmploymentType.FULL_TIME, null, ApplicationStatus.INTERESTED, null, positions, steps);
    }

    /** 마감이 지난 관심·작성중 공고는 목록 조회 시점에 보관함으로 옮겨진다. */
    @Test
    void expiredUnsubmittedPostingIsArchivedOnList() {
        Instant past = Instant.now().minus(1, ChronoUnit.DAYS);
        JobPosting interested = posting(past, ApplicationStatus.INTERESTED);
        JobPosting drafting = posting(past, ApplicationStatus.DRAFTING);
        given(currentUser.id()).willReturn(OWNER_ID);
        given(repository.findPendingAutoArchive(any(), any())).willReturn(List.of(interested, drafting));
        given(repository.findOpen(OWNER_ID)).willReturn(List.of());

        service.findOpen();

        assertThat(interested.isArchived()).isTrue();
        assertThat(drafting.isArchived()).isTrue();
        verify(repository).saveAll(anyList());
    }

    /** 낸 공고는 마감이 지나도 결과를 기다리는 중이라 보관하지 않는다. 작성중은 이제 보관 대상이다. */
    @Test
    void onlyUnsubmittedStatusesAutoArchive() {
        Instant past = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant now = Instant.now();

        assertThat(posting(past, ApplicationStatus.INTERESTED).shouldAutoArchive(now)).isTrue();
        assertThat(posting(past, ApplicationStatus.DRAFTING).shouldAutoArchive(now)).isTrue();
        assertThat(posting(past, ApplicationStatus.SUBMITTED).shouldAutoArchive(now)).isFalse();
        assertThat(posting(past, ApplicationStatus.CLOSED).shouldAutoArchive(now)).isFalse();
        assertThat(posting(null, ApplicationStatus.DOCUMENT_REJECTED).isArchived()).isTrue();
        assertThat(posting(now.plus(1, ChronoUnit.DAYS), ApplicationStatus.INTERESTED).shouldAutoArchive(now)).isFalse();
        assertThat(posting(null, ApplicationStatus.INTERESTED).shouldAutoArchive(now)).isFalse();
    }

    /** 지원완료로 옮기면 보관함에서 꺼낸다. 지원 중인 공고가 보관함에 있으면 앞뒤가 맞지 않는다. */
    @Test
    void submittingRestoresFromArchive() {
        JobPosting archived = posting(Instant.now().minus(1, ChronoUnit.DAYS), ApplicationStatus.DRAFTING);
        archived.archive(Instant.now());

        archived.changeStatus(ApplicationStatus.SUBMITTED);

        assertThat(archived.isArchived()).isFalse();
    }

    @Test
    void rejectedStatusArchivesAndChangingBackRestores() {
        JobPosting posting = posting(Instant.now().plus(1, ChronoUnit.DAYS), ApplicationStatus.SUBMITTED);

        posting.changeStatus(ApplicationStatus.WRITTEN_TEST_REJECTED);
        assertThat(posting.isArchived()).isTrue();

        posting.changeStatus(ApplicationStatus.INTERVIEW_PREP);
        assertThat(posting.isArchived()).isFalse();
    }

    /** 절차 단계는 배열 순서가 진행 순서고, 결과는 seq 로 바꾼다. */
    @Test
    void stepsKeepOrderAndResultChangesBySeq() {
        JobPosting p = new JobPosting(OWNER_ID, new JobPostingAttributes(
                COMPANY_ID, "공채", null, EmploymentType.FULL_TIME, null, ApplicationStatus.SUBMITTED, null,
                List.of(new RecruitmentStep("서류", null, null, null),
                        new RecruitmentStep("인적성", null, null, null),
                        new RecruitmentStep("면접", null, null, null))));

        p.changeStepResult(0, StepResult.PASSED);
        p.changeStepResult(1, StepResult.PASSED);

        assertThat(p.getSteps()).extracting(RecruitmentStep::getName).containsExactly("서류", "인적성", "면접");
        assertThat(p.getSteps()).extracting(RecruitmentStep::getResult)
                .containsExactly(StepResult.PASSED, StepResult.PASSED, StepResult.UPCOMING);
        assertThatThrownBy(() -> p.changeStepResult(3, StepResult.PASSED)).isInstanceOf(IndexOutOfBoundsException.class);
    }

    /** 직접 입력한 회사명은 기존 기업을 찾고(공백·대소문자 무시), 없으면 만든다. */
    @Test
    void directCompanyNameFindsExistingCompany() {
        given(currentUser.id()).willReturn(OWNER_ID);
        Company existing = new Company(OWNER_ID, CompanyAttributes.nameOnly("Naver Cloud"));
        given(companyRepository.findByOwnerIdAndNameKey(OWNER_ID, "navercloud")).willReturn(Optional.of(existing));
        given(repository.save(any())).willAnswer(inv -> inv.getArgument(0));

        service.create(request(null, "  naver  cloud ", List.of(), List.of()));

        verify(companyRepository, never()).save(any());
        ArgumentCaptor<JobPosting> saved = ArgumentCaptor.forClass(JobPosting.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getCompanyId()).isEqualTo(existing.getId());
    }

    @Test
    void unknownCompanyNameCreatesCompany() {
        given(currentUser.id()).willReturn(OWNER_ID);
        given(companyRepository.findByOwnerIdAndNameKey(any(), any())).willReturn(Optional.empty());
        given(companyRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(repository.save(any())).willAnswer(inv -> inv.getArgument(0));

        service.create(request(null, "새 회사", List.of(), List.of()));

        ArgumentCaptor<Company> created = ArgumentCaptor.forClass(Company.class);
        verify(companyRepository).save(created.capture());
        assertThat(created.getValue().getName()).isEqualTo("새 회사");
    }

    /** 직무를 비우면 제목 이름의 직무 하나가 생긴다 — 공고는 직무가 1개 이상. */
    @Test
    void emptyPositionsCreateOneNamedAfterTitle() {
        given(currentUser.id()).willReturn(OWNER_ID);
        given(companyRepository.findByIdAndOwnerId(COMPANY_ID, OWNER_ID))
                .willReturn(Optional.of(new Company(OWNER_ID, CompanyAttributes.nameOnly("회사"))));
        given(repository.save(any())).willAnswer(inv -> inv.getArgument(0));

        service.create(request(COMPANY_ID, null, null, List.of()));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Position>> positions = ArgumentCaptor.forClass(List.class);
        verify(positionRepository).saveAll(positions.capture());
        assertThat(positions.getValue()).extracting(Position::getName).containsExactly("2026 신입 공채");
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
