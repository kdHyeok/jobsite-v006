package com.jobsight.company.company;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** 기업의 주요 사업 한 건. 순서는 Company 의 @OrderColumn 이 지킨다. */
@Embeddable
public class BusinessArea {
    @Column(nullable = false, length = 120)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    protected BusinessArea() {
    }

    public BusinessArea(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
}
