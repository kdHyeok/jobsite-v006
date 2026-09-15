package com.jobsight.company.selfintro.dto;

import com.jobsight.company.selfintro.SelfIntroduction;

import java.time.Instant;
import java.util.UUID;

public record SelfIntroductionResponse(UUID id, UUID resumeId, String question, String answer,
                                       Instant createdAt, Instant updatedAt) {
    public static SelfIntroductionResponse of(SelfIntroduction value) {
        return new SelfIntroductionResponse(value.getId(), value.getResumeId(), value.getQuestion(), value.getAnswer(),
                value.getCreatedAt(), value.getUpdatedAt());
    }
}
