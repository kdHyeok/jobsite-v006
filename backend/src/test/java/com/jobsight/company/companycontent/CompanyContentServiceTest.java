package com.jobsight.company.companycontent;

import com.jobsight.company.auth.CurrentUser;
import com.jobsight.company.common.ResourceNotFoundException;
import com.jobsight.company.company.Company;
import com.jobsight.company.company.CompanyRepository;
import com.jobsight.company.companycontent.dto.CompanyContentRequest;
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
class CompanyContentServiceTest {
    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID COMPANY_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Mock private CompanyContentRepository repository;
    @Mock private CompanyRepository companies;
    @Mock private CurrentUser currentUser;
    @Mock private Company company;

    private CompanyContentService service;

    @BeforeEach
    void setUp() {
        service = new CompanyContentService(repository, companies, currentUser);
        given(currentUser.id()).willReturn(OWNER_ID);
    }

    @Test
    void createRequiresOwnedCompanyAndNormalizesText() {
        var request = new CompanyContentRequest(CompanyContentKind.NEWS, "  새 소식  ", "  요약  ", "  신문사  ", "https://example.com/news");

        assertThatThrownBy(() -> service.create(COMPANY_ID, request))
                .isInstanceOf(ResourceNotFoundException.class);

        given(companies.findByIdAndOwnerId(COMPANY_ID, OWNER_ID)).willReturn(Optional.of(company));
        given(repository.save(any(CompanyContent.class))).willAnswer(invocation -> {
            CompanyContent content = invocation.getArgument(0);
            content.onCreate();
            return content;
        });

        var saved = service.create(COMPANY_ID, request);

        assertThat(saved.title()).isEqualTo("새 소식");
        assertThat(saved.preview()).isEqualTo("요약");
        assertThat(saved.source()).isEqualTo("신문사");
    }
}
