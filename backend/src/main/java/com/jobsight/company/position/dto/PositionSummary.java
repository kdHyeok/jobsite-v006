package com.jobsight.company.position.dto;

import com.jobsight.company.position.Position;

import java.util.UUID;

/** 공고 응답에 실리는 직무 요약. 상세는 /api/positions/{id}. */
public record PositionSummary(UUID id, String name, String team, String headcount, String workLocation) {
    public static PositionSummary of(Position position) {
        return new PositionSummary(position.getId(), position.getName(), position.getTeam(),
                position.getHeadcount(), position.getWorkLocation());
    }
}
