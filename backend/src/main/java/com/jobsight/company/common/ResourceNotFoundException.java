package com.jobsight.company.common;

import java.util.UUID;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(UUID id) {
        super("기업을 찾을 수 없습니다: " + id);
    }
}
