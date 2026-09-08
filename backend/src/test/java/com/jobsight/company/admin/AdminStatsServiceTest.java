package com.jobsight.company.admin;

import com.jobsight.company.admin.dto.UserDataCountResponse;
import com.jobsight.company.common.OwnerCount;
import com.jobsight.company.company.CompanyRepository;
import com.jobsight.company.position.PositionRepository;
import com.jobsight.company.posting.JobPostingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AdminStatsServiceTest {
    private static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Mock private CompanyRepository companies;
    @Mock private JobPostingRepository postings;
    @Mock private PositionRepository positions;

    private AdminStatsService service;

    @BeforeEach
    void setUp() {
        service = new AdminStatsService(companies, postings, positions);
    }

    /** 테이블마다 한 번씩만 센다. 계정 수가 늘어도 쿼리는 3개다. */
    @Test
    void countsEveryOwnerWithThreeQueries() {
        given(companies.countGroupedByOwner()).willReturn(List.of(new OwnerCount(ALICE, 3)));
        given(postings.countGroupedByOwner()).willReturn(List.of(new OwnerCount(ALICE, 12), new OwnerCount(BOB, 5)));
        given(positions.countGroupedByOwner()).willReturn(List.of(new OwnerCount(ALICE, 18), new OwnerCount(BOB, 5)));

        List<UserDataCountResponse> counts = service.countByUser();

        assertThat(counts).containsExactlyInAnyOrder(
                new UserDataCountResponse(ALICE, 3, 12, 18),
                // 기업은 없고 공고만 있는 계정도 빠지지 않는다. 없는 항목은 0.
                new UserDataCountResponse(BOB, 0, 5, 5));
    }

    /** 데이터가 하나도 없는 계정은 응답에 넣지 않는다 — 화면이 0 으로 채운다. */
    @Test
    void ownersWithoutDataAreOmitted() {
        given(companies.countGroupedByOwner()).willReturn(List.of());
        given(postings.countGroupedByOwner()).willReturn(List.of());
        given(positions.countGroupedByOwner()).willReturn(List.of());

        assertThat(service.countByUser()).isEmpty();
    }
}
