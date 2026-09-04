package com.jobsight.company.company;

import java.time.LocalDate;
import java.util.List;

/**
 * 생성·수정 요청이 공통으로 넘기는 정규화된 기업 속성.
 * 생성자 인자를 길게 나열하지 않기 위한 묶음이며, 정규화(trim, 빈 문자열 → null)는 서비스가 끝낸 상태로 넘긴다.
 */
public record CompanyAttributes(
        String name,
        String websiteUrl,
        List<String> industries,
        CompanySize companySize,
        Long annualRevenue,
        Integer employeeCount,
        String address,
        LocalDate foundedOn,
        String summary,
        String memo
) {
}
