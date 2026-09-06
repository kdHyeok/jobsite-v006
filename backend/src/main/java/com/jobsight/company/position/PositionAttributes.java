package com.jobsight.company.position;

import java.util.List;

/** 정규화가 끝난 직무 속성. 직무 페이지의 PUT 이 쓴다. */
public record PositionAttributes(
        String name,
        String team,
        String role,
        String responsibilities,
        String impact,
        String growth,
        String experience,
        String requiredSkills,
        String preferredSkills,
        String headcount,
        String workLocation,
        List<String> techStack
) {
}
