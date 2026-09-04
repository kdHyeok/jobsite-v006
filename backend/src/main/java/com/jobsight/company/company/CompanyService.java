package com.jobsight.company.company;

import com.jobsight.company.auth.CurrentUser;
import com.jobsight.company.common.ResourceNotFoundException;
import com.jobsight.company.company.dto.CompanyCreateRequest;
import com.jobsight.company.company.dto.CompanyResponse;
import com.jobsight.company.company.dto.CompanyUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 모든 조회·변경이 현재 로그인 계정의 소유 데이터로 한정된다.
 * 소유자 id를 컨트롤러에서 받지 않고 여기서 직접 얻으므로 스코프 누락이 발생할 수 없다.
 */
@Service
@Transactional(readOnly = true)
public class CompanyService {
    private final CompanyRepository repository;
    private final CurrentUser currentUser;

    public CompanyService(CompanyRepository repository, CurrentUser currentUser) {
        this.repository = repository;
        this.currentUser = currentUser;
    }

    public List<CompanyResponse> findAll() {
        return repository.findAllByOwnerIdOrderByUpdatedAtDesc(currentUser.id()).stream()
                .map(CompanyResponse::from)
                .toList();
    }

    public CompanyResponse findById(UUID id) {
        return CompanyResponse.from(findOwnedEntity(id));
    }

    @Transactional
    public CompanyResponse create(CompanyCreateRequest request) {
        Company company = new Company(
                currentUser.id(),
                normalizeRequired(request.name()), normalize(request.industry()), normalize(request.location()),
                normalize(request.websiteUrl()), request.status(), normalize(request.summary()), normalize(request.memo())
        );
        return CompanyResponse.from(repository.save(company));
    }

    @Transactional
    public CompanyResponse update(UUID id, CompanyUpdateRequest request) {
        Company company = findOwnedEntity(id);
        company.update(
                normalizeRequired(request.name()), normalize(request.industry()), normalize(request.location()),
                normalize(request.websiteUrl()), request.status(), normalize(request.summary()), normalize(request.memo())
        );
        return CompanyResponse.from(repository.save(company));
    }

    @Transactional
    public void delete(UUID id) {
        repository.delete(findOwnedEntity(id));
    }

    /** 남의 데이터는 존재 자체를 알리지 않도록 403이 아니라 404로 응답한다. */
    private Company findOwnedEntity(UUID id) {
        return repository.findByIdAndOwnerId(id, currentUser.id())
                .orElseThrow(() -> new ResourceNotFoundException(id));
    }

    private String normalizeRequired(String value) {
        return value.trim();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
