package com.jobsight.company.company;

import com.jobsight.company.common.ResourceNotFoundException;
import com.jobsight.company.company.dto.CompanyCreateRequest;
import com.jobsight.company.company.dto.CompanyResponse;
import com.jobsight.company.company.dto.CompanyUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CompanyService {
    private final CompanyRepository repository;

    public CompanyService(CompanyRepository repository) {
        this.repository = repository;
    }

    public List<CompanyResponse> findAll() {
        return repository.findAllByOrderByUpdatedAtDesc().stream()
                .map(CompanyResponse::from)
                .toList();
    }

    public CompanyResponse findById(UUID id) {
        return CompanyResponse.from(findEntity(id));
    }

    @Transactional
    public CompanyResponse create(CompanyCreateRequest request) {
        Company company = new Company(
                normalizeRequired(request.name()), normalize(request.industry()), normalize(request.location()),
                normalize(request.websiteUrl()), request.status(), normalize(request.summary()), normalize(request.memo())
        );
        return CompanyResponse.from(repository.save(company));
    }

    @Transactional
    public CompanyResponse update(UUID id, CompanyUpdateRequest request) {
        Company company = findEntity(id);
        company.update(
                normalizeRequired(request.name()), normalize(request.industry()), normalize(request.location()),
                normalize(request.websiteUrl()), request.status(), normalize(request.summary()), normalize(request.memo())
        );
        return CompanyResponse.from(repository.save(company));
    }

    @Transactional
    public void delete(UUID id) {
        Company company = findEntity(id);
        repository.delete(company);
    }

    private Company findEntity(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(id));
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
