package com.jobsight.company.posting;

import com.jobsight.company.auth.CurrentUser;
import com.jobsight.company.common.ResourceNotFoundException;
import com.jobsight.company.company.Company;
import com.jobsight.company.company.CompanyRepository;
import com.jobsight.company.posting.dto.JobPostingRequest;
import com.jobsight.company.posting.dto.JobPostingResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 모든 조회·변경이 현재 로그인 계정의 소유 데이터로 한정된다.
 * 소유자 id 를 컨트롤러에서 받지 않고 여기서 직접 얻으므로 스코프 누락이 발생할 수 없다.
 */
@Service
@Transactional(readOnly = true)
public class JobPostingService {
    private final JobPostingRepository repository;
    private final CompanyRepository companyRepository;
    private final CurrentUser currentUser;

    public JobPostingService(JobPostingRepository repository,
                             CompanyRepository companyRepository,
                             CurrentUser currentUser) {
        this.repository = repository;
        this.companyRepository = companyRepository;
        this.currentUser = currentUser;
    }

    /**
     * 진행 중 공고를 D-day 순으로. 조회 시점에 마감 지난 관심 공고를 보관함으로 옮긴다.
     * 스케줄러를 두지 않은 이유는 docs/decisions.md 참고.
     */
    @Transactional
    public List<JobPostingResponse> findOpen() {
        UUID ownerId = currentUser.id();
        autoArchiveExpired(ownerId);
        return toResponses(repository.findOpen(ownerId));
    }

    @Transactional
    public List<JobPostingResponse> findArchived() {
        UUID ownerId = currentUser.id();
        autoArchiveExpired(ownerId);
        return toResponses(repository.findArchived(ownerId));
    }

    public JobPostingResponse findById(UUID id) {
        return toResponses(List.of(findOwned(id))).get(0);
    }

    /** 기업 상세의 채용정보 카드. 마감이 지나지 않은 공고만. */
    public List<JobPostingResponse> findOpenByCompany(UUID companyId, String companyName) {
        return repository.findOpenByCompany(currentUser.id(), companyId, Instant.now()).stream()
                .map(posting -> JobPostingResponse.of(posting, companyName))
                .toList();
    }

    @Transactional
    public JobPostingResponse create(JobPostingRequest request) {
        JobPosting posting = new JobPosting(currentUser.id(), toAttributes(request));
        return toResponses(List.of(repository.save(posting))).get(0);
    }

    @Transactional
    public JobPostingResponse update(UUID id, JobPostingRequest request) {
        JobPosting posting = findOwned(id);
        posting.update(toAttributes(request));
        return toResponses(List.of(repository.save(posting))).get(0);
    }

    @Transactional
    public JobPostingResponse changeStage(UUID id, ApplicationStage stage) {
        JobPosting posting = findOwned(id);
        posting.changeStage(stage);
        return toResponses(List.of(repository.save(posting))).get(0);
    }

    @Transactional
    public JobPostingResponse setArchived(UUID id, boolean archived) {
        JobPosting posting = findOwned(id);
        if (archived) {
            posting.archive(Instant.now());
        } else {
            posting.restore();
        }
        return toResponses(List.of(repository.save(posting))).get(0);
    }

    @Transactional
    public void delete(UUID id) {
        repository.delete(findOwned(id));
    }

    private void autoArchiveExpired(UUID ownerId) {
        Instant now = Instant.now();
        List<JobPosting> expired = repository.findExpiredInterested(ownerId, now);
        if (expired.isEmpty()) {
            return;
        }
        expired.forEach(posting -> posting.archive(now));
        repository.saveAll(expired);
    }

    /** 남의 데이터는 존재 자체를 알리지 않도록 403 이 아니라 404 로 응답한다. */
    private JobPosting findOwned(UUID id) {
        return repository.findByIdAndOwnerId(id, currentUser.id())
                .orElseThrow(() -> new ResourceNotFoundException(id));
    }

    /**
     * 연결된 기업 이름을 한 번에 조회해 채운다.
     * 공고마다 기업을 조회하면 목록에서 N+1 이 된다.
     */
    private List<JobPostingResponse> toResponses(List<JobPosting> postings) {
        List<UUID> companyIds = postings.stream()
                .map(JobPosting::getCompanyId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<UUID, String> namesById = new HashMap<>();
        if (!companyIds.isEmpty()) {
            companyRepository.findAllByIdInAndOwnerId(companyIds, currentUser.id())
                    .forEach(company -> namesById.put(company.getId(), company.getName()));
        }

        return postings.stream()
                .map(posting -> JobPostingResponse.of(posting, resolveName(posting, namesById)))
                .toList();
    }

    private String resolveName(JobPosting posting, Map<UUID, String> namesById) {
        if (posting.getCompanyId() != null) {
            String linked = namesById.get(posting.getCompanyId());
            if (linked != null) {
                return linked;
            }
        }
        return posting.getCompanyNameSnapshot();
    }

    private JobPostingAttributes toAttributes(JobPostingRequest request) {
        UUID companyId = request.companyId();
        String companyName = normalize(request.companyName());

        if (companyId != null) {
            // 남의 기업에 공고를 붙일 수 없다. 존재를 알리지 않도록 404.
            Company company = companyRepository.findByIdAndOwnerId(companyId, currentUser.id())
                    .orElseThrow(() -> new ResourceNotFoundException(companyId));
            companyName = company.getName();
        }

        return new JobPostingAttributes(
                companyId,
                companyName,
                request.position().trim(),
                normalize(request.postingUrl()),
                request.employmentType(),
                request.deadlineAt(),
                request.stage(),
                normalize(request.headcount()),
                normalize(request.workLocation()),
                normalize(request.qualifications()),
                normalize(request.responsibilities()),
                normalize(request.requiredSkills())
        );
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
