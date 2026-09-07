package com.jobsight.company.resume.dto;

import com.jobsight.company.resume.Resume;

import java.time.Instant;
import java.util.UUID;

/** 목록용. content 없음. */
public record ResumeSummary(UUID id, String name, Instant createdAt, Instant updatedAt) {
    public static ResumeSummary of(Resume resume) {
        return new ResumeSummary(resume.getId(), resume.getName(), resume.getCreatedAt(), resume.getUpdatedAt());
    }
}
