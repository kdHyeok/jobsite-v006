package com.jobsight.company.attachment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * 이력서 증빙 파일. 바이트를 행에 그대로 담는다 — 개인 도구에서 파일 몇십 개를 위해
 * 오브젝트 스토리지를 붙이지 않는다. content_type 은 클라이언트가 말한 값이 아니라
 * AttachmentService 가 앞머리 바이트로 판정한 값이다(docs/attachments.md 불변 조건 2).
 */
@Entity
@Table(name = "attachments")
public class Attachment {
    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Column(nullable = false, length = 260)
    private String filename;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private int sizeBytes;

    @Column(nullable = false)
    private byte[] data;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Attachment() {
    }

    public Attachment(UUID ownerId, String filename, String contentType, byte[] data) {
        this.id = UUID.randomUUID();
        this.ownerId = ownerId;
        this.filename = filename;
        this.contentType = contentType;
        this.data = data;
        this.sizeBytes = data.length;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public String getFilename() { return filename; }
    public String getContentType() { return contentType; }
    public int getSizeBytes() { return sizeBytes; }
    public byte[] getData() { return data; }
    public Instant getCreatedAt() { return createdAt; }
}
