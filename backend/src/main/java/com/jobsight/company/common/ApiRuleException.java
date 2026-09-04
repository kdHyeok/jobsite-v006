package com.jobsight.company.common;

import org.springframework.http.HttpStatus;

/**
 * 업무 규칙 위반을 상태코드와 코드값으로 함께 전달하는 예외.
 * 규칙마다 예외 클래스를 만들지 않기 위해 하나로 둔다.
 */
public class ApiRuleException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public ApiRuleException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
