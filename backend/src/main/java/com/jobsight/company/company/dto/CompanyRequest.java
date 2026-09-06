package com.jobsight.company.company.dto;

import com.jobsight.company.company.CompanySize;
import com.jobsight.company.company.RevenueUnit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * 기업 생성·수정 요청. 두 요청의 필드가 같아 하나로 쓴다.
 * 채용정보(공고 리스트)는 사용자 입력이 아니라 공고 쪽에서 채워지므로 여기 없다.
 */
public record CompanyRequest(
        @NotBlank(message = "기업명은 필수입니다.")
        @Size(max = 120, message = "기업명은 120자 이하여야 합니다.")
        String name,

        @Size(max = 500, message = "웹사이트 URL은 500자 이하여야 합니다.")
        @Pattern(regexp = "^$|https?://.+", message = "웹사이트는 http 또는 https URL이어야 합니다.")
        String websiteUrl,

        @Size(max = 10, message = "업종은 10개 이하로 입력해 주세요.")
        List<@Size(max = 60, message = "업종은 60자 이하여야 합니다.") String> industries,

        CompanySize companySize,

        @PositiveOrZero(message = "매출액은 0 이상이어야 합니다.")
        Long annualRevenue,

        RevenueUnit revenueUnit,

        @PositiveOrZero(message = "사원수는 0 이상이어야 합니다.")
        Integer employeeCount,

        @Size(max = 200, message = "주소는 200자 이하여야 합니다.")
        String address,

        /** 설립연월. 화면은 연월만 받고 1일로 채워 보낸다. */
        LocalDate foundedOn,

        @Size(max = 2000, message = "간략 소개는 2,000자 이하여야 합니다.")
        String summary,

        @Size(max = 5000, message = "기업 복지는 5,000자 이하여야 합니다.")
        String benefits,

        @Size(max = 5000, message = "메모는 5,000자 이하여야 합니다.")
        String memo
) {
}
