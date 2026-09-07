package com.jobsight.company.resume;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;

/**
 * 행이 있는 8개 섹션. 상수 이름이 JSON 키이자 프런트 SECTIONS 의 key 다(소문자 그대로).
 * MCP 스키마는 enum 값을 그대로 나열하므로 도구 사용자가 섹션 이름을 추측하지 않는다.
 */
public enum ResumeSection {
    educations(ResumeContent.Education.class),
    trainings(ResumeContent.Training.class),
    activities(ResumeContent.Activity.class),
    experiences(ResumeContent.Experience.class),
    awards(ResumeContent.Award.class),
    certificates(ResumeContent.Certificate.class),
    skills(ResumeContent.Skill.class),
    projects(ResumeContent.Project.class);

    private final Class<?> rowType;

    ResumeSection(Class<?> rowType) {
        this.rowType = rowType;
    }

    public String key() {
        return name();
    }

    /** 이 섹션 행의 record. 행 도구가 다른 섹션 필드를 거부할 때 기준이 된다. */
    public Class<?> rowType() {
        return rowType;
    }

    public List<String> fields() {
        return Arrays.stream(rowType.getRecordComponents()).map(RecordComponent::getName).toList();
    }
}
