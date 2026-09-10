package com.jobsight.company.company.dto;

import com.jobsight.company.company.BusinessArea;

public record BusinessAreaResponse(String name, String description) {
    public static BusinessAreaResponse of(BusinessArea area) {
        return new BusinessAreaResponse(area.getName(), area.getDescription());
    }
}
