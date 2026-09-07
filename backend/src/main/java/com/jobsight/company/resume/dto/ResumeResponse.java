package com.jobsight.company.resume.dto;

import com.jobsight.company.resume.Resume;
import com.jobsight.company.resume.ResumeContent;

import java.time.Instant;
import java.util.UUID;

public record ResumeResponse(UUID id, String name, ResumeContent content, Instant createdAt, Instant updatedAt) {
    public static ResumeResponse of(Resume resume, ResumeContent content) {
        return new ResumeResponse(resume.getId(), resume.getName(), content, resume.getCreatedAt(), resume.getUpdatedAt());
    }
}
