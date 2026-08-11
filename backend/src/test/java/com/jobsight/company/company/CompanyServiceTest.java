package com.jobsight.company.company;

import com.jobsight.company.common.ResourceNotFoundException;
import com.jobsight.company.company.dto.CompanyCreateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {
    @Mock
    private CompanyRepository repository;
    private CompanyService service;

    @BeforeEach
    void setUp() {
        service = new CompanyService(repository);
    }

    @Test
    void createNormalizesBlankOptionalFields() {
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
        given(repository.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());
    }
}
