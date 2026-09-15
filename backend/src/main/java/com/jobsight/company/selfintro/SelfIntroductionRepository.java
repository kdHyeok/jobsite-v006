package com.jobsight.company.selfintro;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SelfIntroductionRepository extends JpaRepository<SelfIntroduction, UUID> {
    Optional<SelfIntroduction> findByIdAndOwnerId(UUID id, UUID ownerId);
    List<SelfIntroduction> findAllByOwnerIdOrderByUpdatedAtDesc(UUID ownerId);
}
