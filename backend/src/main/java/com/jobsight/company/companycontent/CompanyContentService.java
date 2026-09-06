package com.jobsight.company.companycontent;

import com.jobsight.company.auth.CurrentUser;
import com.jobsight.company.common.ResourceNotFoundException;
import com.jobsight.company.company.CompanyRepository;
import com.jobsight.company.companycontent.dto.CompanyContentRequest;
import com.jobsight.company.companycontent.dto.CompanyContentResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CompanyContentService {
    private final CompanyContentRepository repository;
    private final CompanyRepository companies;
    private final CurrentUser currentUser;

    public CompanyContentService(CompanyContentRepository repository, CompanyRepository companies, CurrentUser currentUser) {
        this.repository = repository;
        this.companies = companies;
        this.currentUser = currentUser;
    }

    public List<CompanyContentResponse> findAll(UUID companyId) {
        UUID ownerId = currentUser.id();
        requireOwnedCompany(companyId, ownerId);
        return repository.findAllByCompanyIdAndOwnerIdOrderByUpdatedAtDesc(companyId, ownerId).stream()
                .map(CompanyContentResponse::of)
                .toList();
    }

    @Transactional
    public CompanyContentResponse create(UUID companyId, CompanyContentRequest request) {
        UUID ownerId = currentUser.id();
        requireOwnedCompany(companyId, ownerId);
        CompanyContent content = new CompanyContent(ownerId, companyId, request.kind(), request.title().trim(),
                normalize(request.preview()), normalize(request.source()), request.url().trim());
        return CompanyContentResponse.of(repository.save(content));
    }

    @Transactional
    public CompanyContentResponse update(UUID companyId, UUID id, CompanyContentRequest request) {
        CompanyContent content = findOwned(companyId, id);
        content.update(request.kind(), request.title().trim(), normalize(request.preview()),
                normalize(request.source()), request.url().trim());
        return CompanyContentResponse.of(repository.save(content));
    }

    @Transactional
    public void delete(UUID companyId, UUID id) {
        repository.delete(findOwned(companyId, id));
    }

    private CompanyContent findOwned(UUID companyId, UUID id) {
        return repository.findByIdAndCompanyIdAndOwnerId(id, companyId, currentUser.id())
                .orElseThrow(() -> new ResourceNotFoundException(id));
    }

    private void requireOwnedCompany(UUID companyId, UUID ownerId) {
        companies.findByIdAndOwnerId(companyId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException(companyId));
    }

    private static String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
