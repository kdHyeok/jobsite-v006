package com.jobsight.company.position;

import com.jobsight.company.common.OwnerCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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

    /** 관리자 화면의 계정별 등록 수. 개수만 낸다 — 내용은 내보내지 않는다(docs/admin.md). */
    @Query("select new com.jobsight.company.common.OwnerCount(p.ownerId, count(p)) from Position p group by p.ownerId")
    List<OwnerCount> countGroupedByOwner();
}
