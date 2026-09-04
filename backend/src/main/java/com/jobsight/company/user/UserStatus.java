package com.jobsight.company.user;

public enum UserStatus {
    /** 가입 신청됨. 관리자 승인 전이며 로그인할 수 없다. */
    PENDING,
    /** 승인됨. 로그인 가능한 유일한 상태. */
    ACTIVE,
    /** 관리자가 일시 정지시킴. */
    SUSPENDED,
    /** 관리자가 가입 신청을 거절함. */
    REJECTED;

    public boolean canLogIn() {
        return this == ACTIVE;
    }
}
