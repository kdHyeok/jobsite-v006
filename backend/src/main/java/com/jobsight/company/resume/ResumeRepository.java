package com.jobsight.company.resume;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResumeRepository extends JpaRepository<Resume, UUID> {

    /** 소유자 조건을 쿼리에 넣어 남의 행은 애초에 조회되지 않게 한다. findById 후 비교 금지. */
    Optional<Resume> findByIdAndOwnerId(UUID id, UUID ownerId);

    List<Resume> findAllByOwnerIdOrderByUpdatedAtDesc(UUID ownerId);
}
