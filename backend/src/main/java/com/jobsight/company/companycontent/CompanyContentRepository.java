package com.jobsight.company.companycontent;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanyContentRepository extends JpaRepository<CompanyContent, UUID> {
    List<CompanyContent> findAllByCompanyIdAndOwnerIdOrderByUpdatedAtDesc(UUID companyId, UUID ownerId);

    Optional<CompanyContent> findByIdAndCompanyIdAndOwnerId(UUID id, UUID companyId, UUID ownerId);
}
