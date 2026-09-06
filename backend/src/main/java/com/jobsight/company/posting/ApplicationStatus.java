package com.jobsight.company.posting;

/**
 * 내 참여 상태. 회사의 채용 절차(RecruitmentStep)와는 다른 축이다 — docs/job-postings.md.
 * 선언 순서가 진행 정도를 뜻한다.
 */
public enum ApplicationStatus {
    INTERESTED,
    DRAFTING,
    SUBMITTED,
    WRITTEN_TEST_PREP,
    INTERVIEW_PREP,
    ACCEPTED,
    DOCUMENT_REJECTED,
    WRITTEN_TEST_REJECTED,
    INTERVIEW_REJECTED,
    /** V10 이전의 포괄적인 종료 값. 새 입력에는 쓰지 않고 기존 데이터만 보관한다. */
    CLOSED;

    /**
     * 마감이 지나면 자동 보관할 상태인지.
     * 아직 내지 않은 공고(관심·작성중)만. 낸 공고는 마감 뒤에도 결과를 기다리는 살아있는 정보다.
     */
    public boolean isAutoArchivable() {
        return this == INTERESTED || this == DRAFTING;
    }

    public boolean isRejectedOrClosed() {
        return this == DOCUMENT_REJECTED
                || this == WRITTEN_TEST_REJECTED
                || this == INTERVIEW_REJECTED
                || this == CLOSED;
    }
}
