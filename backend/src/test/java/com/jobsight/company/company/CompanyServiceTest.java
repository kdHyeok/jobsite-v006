package com.jobsight.company.company;

import com.jobsight.company.auth.CurrentUser;
import com.jobsight.company.common.ResourceNotFoundException;
import com.jobsight.company.company.dto.CompanyCreateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {
    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock
    private CompanyRepository repository;
    @Mock
    private CurrentUser currentUser;

    private CompanyService service;

    @BeforeEach
    void setUp() {
        service = new CompanyService(repository, currentUser);
    }

    @Test
    void createNormalizesBlankOptionalFields() {
        given(currentUser.id()).willReturn(OWNER_ID);
        given(repository.save(any(Company.class))).willAnswer(invocation -> {
            Company company = invocation.getArgument(0);
            company.onCreate();
            return company;
        });

        var response = service.create(new CompanyCreateRequest(
                "  테스트 기업  ", "  ", null, "", CompanyStatus.INTERESTED, " 요약 ", "  "
        ));

        assertThat(response.name()).isEqualTo("테스트 기업");
        assertThat(response.industry()).isNull();
        assertThat(response.websiteUrl()).isNull();
        assertThat(response.summary()).isEqualTo("요약");
        assertThat(response.memo()).isNull();
    }

    @Test
    void missingCompanyThrowsNotFound() {
        UUID id = UUID.randomUUID();
        given(currentUser.id()).willReturn(OWNER_ID);
        given(repository.findByIdAndOwnerId(id, OWNER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    /**
     * 계정별 데이터 분리의 핵심 검증.
     * 다른 계정 소유의 id로 접근하면 소유자 조건이 붙은 쿼리가 비어 돌아오므로
     * 읽기·수정·삭제 모두 존재를 알리지 않고 404가 된다.
     */
    @Test
    void otherOwnersCompanyIsNotReachable() {
        UUID otherOwnersCompanyId = UUID.randomUUID();
        given(currentUser.id()).willReturn(OWNER_ID);
        given(repository.findByIdAndOwnerId(otherOwnersCompanyId, OWNER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(otherOwnersCompanyId))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.delete(otherOwnersCompanyId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listIsScopedToCurrentOwner() {
        given(currentUser.id()).willReturn(OWNER_ID);
        given(repository.findAllByOwnerIdOrderByUpdatedAtDesc(OWNER_ID)).willReturn(List.of());

        assertThat(service.findAll()).isEmpty();
    }
}
