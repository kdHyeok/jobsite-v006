package com.jobsight.company.position.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PositionRequest(
        @NotBlank(message = "직무 이름은 필수입니다.")
        @Size(max = 160, message = "직무 이름은 160자 이하여야 합니다.")
        String name,
        @Size(max = 120, message = "소속 팀은 120자 이하여야 합니다.")
        String team,
        @Size(max = 200, message = "담당 역할은 200자 이하여야 합니다.")
        String role,
        @Size(max = 5000) String responsibilities,
        @Size(max = 5000) String impact,
        @Size(max = 5000) String growth,
        @Size(max = 5000) String experience,
        @Size(max = 5000) String requiredSkills,
        @Size(max = 5000) String preferredSkills,
        @Size(max = 60, message = "모집인원은 60자 이하여야 합니다.")
        String headcount,
        @Size(max = 160, message = "근무지역은 160자 이하여야 합니다.")
        String workLocation,
        @Size(max = 30, message = "기술 스택은 30개 이하여야 합니다.")
        List<@Size(max = 60, message = "기술 이름은 60자 이하여야 합니다.") String> techStack
) {
}
