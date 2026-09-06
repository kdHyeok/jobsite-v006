package com.jobsight.company.posting.dto;

import com.jobsight.company.posting.RecruitmentStep;
import com.jobsight.company.posting.StepResult;

import java.time.Instant;

public record StepResponse(
        int seq,
        String name,
        StepResult result,
        Instant scheduledAt,
        String memo
) {
    public static StepResponse of(int seq, RecruitmentStep step) {
        return new StepResponse(seq, step.getName(), step.getResult(), step.getScheduledAt(), step.getMemo());
    }
}
