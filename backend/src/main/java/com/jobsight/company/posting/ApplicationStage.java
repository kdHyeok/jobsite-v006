package com.jobsight.company.posting;

/** 지원단계. 선언 순서가 진행 정도를 뜻한다. */
public enum ApplicationStage {
    INTERESTED,
    DRAFTING,
    SUBMITTED,
    CODING_TEST,
    INTERVIEW,
    AWAITING_RESULT;

    /**
     * 마감이 지나면 자동 보관할 단계인지.
     * 아직 지원하지 않은 관심 공고만 해당한다. 지원한 공고는 마감 뒤에도 결과를 기다리는 살아있는 정보다.
     */
    public boolean isAutoArchivable() {
        return this == INTERESTED;
    }
}
