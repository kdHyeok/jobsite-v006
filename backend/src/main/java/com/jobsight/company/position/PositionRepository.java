package com.jobsight.company.position;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PositionRepository extends JpaRepository<Position, UUID> {

    Optional<Position> findByIdAndOwnerId(UUID id, UUID ownerId);

    List<Position> findAllByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    List<Position> findAllByPostingIdAndOwnerId(UUID postingId, UUID ownerId);

    List<Position> findAllByPostingIdInAndOwnerIdOrderByCreatedAtAsc(Collection<UUID> postingIds, UUID ownerId);

    List<Position> findAllByIdInAndOwnerId(Collection<UUID> ids, UUID ownerId);

    long countByPostingIdAndOwnerId(UUID postingId, UUID ownerId);
}
