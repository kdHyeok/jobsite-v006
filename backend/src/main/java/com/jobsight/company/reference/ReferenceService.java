package com.jobsight.company.reference;

import com.jobsight.company.auth.CurrentUser;
import com.jobsight.company.common.ResourceNotFoundException;
import com.jobsight.company.position.PositionRepository;
import com.jobsight.company.reference.dto.ReferenceRequest;
import com.jobsight.company.reference.dto.ReferenceResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ReferenceService {
    private final ReferenceRepository repository;
    private final PositionRepository positionRepository;
    private final CurrentUser currentUser;

    public ReferenceService(ReferenceRepository repository, PositionRepository positionRepository, CurrentUser currentUser) {
        this.repository = repository;
        this.positionRepository = positionRepository;
        this.currentUser = currentUser;
    }

    /** q 는 제목·메모 부분 일치. 직무 드로어의 "기존에서 검색" 이 쓴다. */
    public List<ReferenceResponse> findAll(String q) {
        String needle = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        return repository.findAllByOwnerIdOrderByUpdatedAtDesc(currentUser.id()).stream()
                .filter(item -> needle.isEmpty()
                        || item.getTitle().toLowerCase(Locale.ROOT).contains(needle)
                        || (item.getMemo() != null && item.getMemo().toLowerCase(Locale.ROOT).contains(needle)))
                .map(ReferenceResponse::of)
                .toList();
    }

    @Transactional
    public ReferenceResponse create(ReferenceRequest request) {
        ReferenceItem item = new ReferenceItem(currentUser.id(), request.kind(), request.title().trim(),
                normalize(request.url()), normalize(request.memo()), ownedPositionOrNull(request.relatedPositionId()));
        return ReferenceResponse.of(repository.save(item));
    }

    @Transactional
    public ReferenceResponse update(UUID id, ReferenceRequest request) {
        ReferenceItem item = findOwned(id);
        item.update(request.kind(), request.title().trim(), normalize(request.url()), normalize(request.memo()),
                ownedPositionOrNull(request.relatedPositionId()));
        return ReferenceResponse.of(repository.save(item));
    }

    /** 연결(position_references)은 FK ON DELETE CASCADE 로 함께 사라진다. */
    @Transactional
    public void delete(UUID id) {
        repository.delete(findOwned(id));
    }

    private ReferenceItem findOwned(UUID id) {
        return repository.findByIdAndOwnerId(id, currentUser.id())
                .orElseThrow(() -> new ResourceNotFoundException(id));
    }

    /** 남의 직무를 참고 직무로 가리킬 수 없다. 존재를 알리지 않도록 404. */
    private UUID ownedPositionOrNull(UUID positionId) {
        if (positionId == null) {
            return null;
        }
        return positionRepository.findByIdAndOwnerId(positionId, currentUser.id())
                .orElseThrow(() -> new ResourceNotFoundException(positionId))
                .getId();
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
