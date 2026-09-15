package com.jobsight.company.adminrequest;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdminRequestRepository extends JpaRepository<AdminRequest, UUID> {
    List<AdminRequest> findAllByOwnerIdOrderByUpdatedAtDesc(UUID ownerId);
    List<AdminRequest> findAllByOrderByCreatedAtDesc();
    Optional<AdminRequest> findByIdAndOwnerId(UUID id, UUID ownerId);
}
