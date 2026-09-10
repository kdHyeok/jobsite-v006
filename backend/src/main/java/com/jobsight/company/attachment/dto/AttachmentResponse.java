package com.jobsight.company.attachment.dto;

import java.util.UUID;

/** 첨부 메타. 이력서 행은 이 id 문자열만 저장한다. */
public record AttachmentResponse(UUID id, String filename, String contentType, int size) {
}
