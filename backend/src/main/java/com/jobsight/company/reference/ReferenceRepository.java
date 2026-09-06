package com.jobsight.company.reference;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReferenceRepository extends JpaRepository<ReferenceItem, UUID> {

    Optional<ReferenceItem> findByIdAndOwnerId(UUID id, UUID ownerId);

    List<ReferenceItem> findAllByOwnerIdOrderByUpdatedAtDesc(UUID ownerId);

    List<ReferenceItem> findAllByIdInAndOwnerId(Collection<UUID> ids, UUID ownerId);
}
