package com.jobsight.company.resume;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 이력서 문서의 모양. JSON 으로 저장되고 요청·응답에 그대로 실린다.
 * 프런트 types/resume.ts 의 SECTIONS 와 키가 같아야 한다 — Jackson 은 모르는 키를 조용히 버린다.
 * 연월은 사용자가 쓴 표현("2024.03", "현재")을 잃지 않도록 문자열이다.
 */
public record ResumeContent(
        @Valid BasicInfo basic,
        @Size(max = 50, message = "학력은 50개 이하여야 합니다.") List<@Valid Education> educations,
        @Size(max = 50, message = "교육이수는 50개 이하여야 합니다.") List<@Valid Training> trainings,
        @Size(max = 50, message = "대내외활동은 50개 이하여야 합니다.") List<@Valid Activity> activities,
        @Size(max = 50, message = "경력은 50개 이하여야 합니다.") List<@Valid Experience> experiences,
        @Size(max = 50, message = "수상은 50개 이하여야 합니다.") List<@Valid Award> awards,
        @Size(max = 50, message = "자격증은 50개 이하여야 합니다.") List<@Valid Certificate> certificates,
        @Size(max = 50, message = "SW 역량은 50개 이하여야 합니다.") List<@Valid Skill> skills,
        @Size(max = 50, message = "프로젝트는 50개 이하여야 합니다.") List<@Valid Project> projects
) {
    private static final int SHORT = 200;
    private static final int URL = 500;
    private static final int LONG = 5000;

    public static ResumeContent empty() {
        return new ResumeContent(BasicInfo.empty(),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }

    /** null 을 빈 값으로. 저장된 JSON 이 항상 같은 모양이라 읽는 쪽이 null 검사를 안 한다. */
    public ResumeContent normalized() {
        return new ResumeContent(
                basic == null ? BasicInfo.empty() : basic,
                orEmpty(educations), orEmpty(trainings), orEmpty(activities), orEmpty(experiences),
                orEmpty(awards), orEmpty(certificates), orEmpty(skills), orEmpty(projects));
    }

    private static <T> List<T> orEmpty(List<T> list) {
        return list == null ? List.of() : list;
    }

    public record BasicInfo(
            @Size(max = SHORT) String name,
            @Size(max = SHORT) String phone,
            @Size(max = SHORT) String birthDate,
            @Size(max = SHORT) String email,
            @Size(max = URL) String address,
            @Size(max = URL) String portfolioUrl,
            @Size(max = URL) String githubUrl
    ) {
        public static BasicInfo empty() {
            return new BasicInfo(null, null, null, null, null, null, null);
        }
    }

    public record Education(
            @Size(max = SHORT) String startYm,
            @Size(max = SHORT) String endYm,
            @Size(max = SHORT) String school,
            @Size(max = SHORT) String major,
            @Size(max = SHORT) String gpa
    ) {
    }

    public record Training(
            @Size(max = SHORT) String name,
            @Size(max = SHORT) String institution,
            @Size(max = SHORT) String startYm,
            @Size(max = SHORT) String endYm,
            @Size(max = LONG) String description
    ) {
    }

    public record Activity(
            @Size(max = SHORT) String name,
            @Size(max = SHORT) String organizer,
            @Size(max = SHORT) String startYm,
            @Size(max = SHORT) String endYm,
            @Size(max = LONG) String description
    ) {
    }

    public record Experience(
            @Size(max = SHORT) String company,
            @Size(max = SHORT) String startYm,
            @Size(max = SHORT) String endYm,
            @Size(max = LONG) String description
    ) {
    }

    public record Award(
            @Size(max = SHORT) String name,
            @Size(max = SHORT) String issuer,
            @Size(max = SHORT) String awardedYm
    ) {
    }

    public record Certificate(
            @Size(max = SHORT) String name,
            @Size(max = SHORT) String issuer,
            @Size(max = SHORT) String acquiredYm
    ) {
    }

    public record Skill(
            @Size(max = SHORT) String name,
            @Size(max = SHORT) String level,
            @Size(max = LONG) String description
    ) {
    }

    public record Project(
            @Size(max = SHORT) String name,
            @Size(max = SHORT) String headcount,
            @Size(max = SHORT) String startYm,
            @Size(max = SHORT) String endYm,
            @Size(max = LONG) String summary,
            @Size(max = URL) String techStack,
            @Size(max = URL) String role,
            @Size(max = LONG) String outcome,
            @Size(max = LONG) String description,
            @Size(max = URL) String url
    ) {
    }
}
