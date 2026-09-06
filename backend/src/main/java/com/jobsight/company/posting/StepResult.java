package com.jobsight.company.posting;

/** 절차 한 단계에서 내가 어디까지 왔나. 화면의 노드 클릭이 이 순서로 순환한다. */
public enum StepResult {
    UPCOMING,
    IN_PROGRESS,
    PASSED,
    FAILED
}
