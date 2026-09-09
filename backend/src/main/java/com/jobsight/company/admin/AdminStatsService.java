package com.jobsight.company.admin;

import com.jobsight.company.admin.dto.UserDataCountResponse;
import com.jobsight.company.common.OwnerCount;
import com.jobsight.company.company.CompanyRepository;
import com.jobsight.company.position.PositionRepository;
import com.jobsight.company.posting.JobPostingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 관리자용 계정별 등록 데이터 개수. 소유자 격리의 좁은 예외이고, 예외는 집계 숫자에 한정된다 —
 * 어떤 계정의 기업명·공고 제목도 여기서 나가지 않는다(docs/admin.md 불변 조건 1).
 *
 * 접근 제어는 SecurityConfig 의 hasRole('ADMIN') 이 ApiPaths.ADMIN 전체에 걸어 둔다.
 */
@Service
@Transactional(readOnly = true)
public class AdminStatsService {
    private final CompanyRepository companies;
    private final JobPostingRepository postings;
    private final PositionRepository positions;

    public AdminStatsService(CompanyRepository companies, JobPostingRepository postings,
                             PositionRepository positions) {
        this.companies = companies;
        this.postings = postings;
        this.positions = positions;
    }

    /**
     * 쿼리 3개로 끝난다 — 계정 수와 무관하다. 계정마다 세면 N 명에 3N 쿼리가 된다.
     * 데이터가 하나도 없는 계정은 빠진다. 화면이 0 으로 채운다.
     */
    public List<UserDataCountResponse> countByUser() {
        Map<UUID, Long> companyCounts = toMap(companies.countGroupedByOwner());
        Map<UUID, Long> postingCounts = toMap(postings.countGroupedByOwner());
        Map<UUID, Long> positionCounts = toMap(positions.countGroupedByOwner());

        Set<UUID> owners = new LinkedHashSet<>();
        owners.addAll(companyCounts.keySet());
        owners.addAll(postingCounts.keySet());
        owners.addAll(positionCounts.keySet());

        return owners.stream()
                .map(owner -> new UserDataCountResponse(owner,
                        companyCounts.getOrDefault(owner, 0L),
                        postingCounts.getOrDefault(owner, 0L),
                        positionCounts.getOrDefault(owner, 0L)))
                .toList();
    }

    private static Map<UUID, Long> toMap(List<OwnerCount> rows) {
        Map<UUID, Long> counts = new LinkedHashMap<>();
        rows.forEach(row -> counts.put(row.ownerId(), row.count()));
        return counts;
    }
}
