package com.jobsight.company.attachment;

import com.jobsight.company.attachment.dto.AttachmentResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {

    /** 소유자 조건을 쿼리에 넣는다. findById 후 비교하면 비교를 빠뜨리는 순간 유출이다. */
    Optional<Attachment> findByIdAndOwnerId(UUID id, UUID ownerId);

    /** 메타만 필요할 때. bytea 를 읽어 오지 않는다. */
    @Query("""
            select new com.jobsight.company.attachment.dto.AttachmentResponse(a.id, a.filename, a.contentType, a.sizeBytes)
              from Attachment a
             where a.id = :id and a.ownerId = :ownerId
            """)
    Optional<AttachmentResponse> findMeta(@Param("id") UUID id, @Param("ownerId") UUID ownerId);
}
