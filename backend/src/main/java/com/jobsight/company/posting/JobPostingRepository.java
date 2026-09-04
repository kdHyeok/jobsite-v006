package com.jobsight.company.posting;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobPostingRepository extends JpaRepository<JobPosting, UUID> {

    /**
     * 소유자 조건을 쿼리에 포함시켜 남의 행은 애초에 조회되지 않게 한다.
     * findById 후 소유자를 비교하는 방식은 비교를 빠뜨리면 그대로 유출이 되므로 쓰지 않는다.
     */
    Optional<JobPosting> findByIdAndOwnerId(UUID id, UUID ownerId);

    /** 진행 중 공고를 D-day 순으로. 마감 없는(상시) 공고는 맨 뒤. */
    @Query("""
            select p from JobPosting p
             where p.ownerId = :ownerId and p.archivedAt is null
             order by p.deadlineAt asc nulls last, p.createdAt desc
            """)
    List<JobPosting> findOpen(@Param("ownerId") UUID ownerId);

    @Query("""
            select p from JobPosting p
             where p.ownerId = :ownerId and p.archivedAt is not null
             order by p.archivedAt desc
            """)
    List<JobPosting> findArchived(@Param("ownerId") UUID ownerId);

    /** 자동 보관 대상: 마감이 지난 관심 공고. */
    @Query("""
            select p from JobPosting p
             where p.ownerId = :ownerId
               and p.archivedAt is null
               and p.stage = com.jobsight.company.posting.ApplicationStage.INTERESTED
               and p.deadlineAt is not null and p.deadlineAt < :now
            """)
    List<JobPosting> findExpiredInterested(@Param("ownerId") UUID ownerId, @Param("now") Instant now);

    /** 기업 상세의 채용정보 카드. 마감이 지나지 않은 것만. */
    @Query("""
            select p from JobPosting p
             where p.ownerId = :ownerId
               and p.companyId = :companyId
               and p.archivedAt is null
               and (p.deadlineAt is null or p.deadlineAt >= :now)
             order by p.deadlineAt asc nulls last
            """)
    List<JobPosting> findOpenByCompany(@Param("ownerId") UUID ownerId,
                                       @Param("companyId") UUID companyId,
                                       @Param("now") Instant now);
}
