package com.jobsight.company.company;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {

    List<Company> findAllByOwnerIdOrderByUpdatedAtDesc(UUID ownerId);

    /**
     * 소유자 조건을 쿼리에 포함시켜, 남의 행은 애초에 조회되지 않게 한다.
     * findById 후 소유자를 비교하는 방식은 비교를 빠뜨리면 그대로 유출이 되므로 쓰지 않는다.
     */
    Optional<Company> findByIdAndOwnerId(UUID id, UUID ownerId);

    long countByOwnerId(UUID ownerId);
}
