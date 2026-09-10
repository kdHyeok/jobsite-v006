package com.jobsight.company.attachment;

import com.jobsight.company.attachment.dto.AttachmentResponse;
import com.jobsight.company.auth.CurrentUser;
import com.jobsight.company.common.ApiRuleException;
import com.jobsight.company.common.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;

/** 이력서 증빙 파일. 모든 조회는 소유자로 한정하고, 타인 것은 404 다. */
@Service
@Transactional(readOnly = true)
public class AttachmentService {
    /** 10MB. application.yml 의 multipart 상한·nginx client_max_body_size 와 함께 움직인다. */
    public static final int MAX_BYTES = 10 * 1024 * 1024;

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PDF = {'%', 'P', 'D', 'F', '-'};

    private final AttachmentRepository repository;
    private final CurrentUser currentUser;

    public AttachmentService(AttachmentRepository repository, CurrentUser currentUser) {
        this.repository = repository;
        this.currentUser = currentUser;
    }

    @Transactional
    public AttachmentResponse upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiRuleException(HttpStatus.BAD_REQUEST, "EMPTY_FILE", "빈 파일은 올릴 수 없습니다.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ApiRuleException(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE",
                    "파일은 10MB 이하여야 합니다.");
        }
        byte[] data = read(file);
        Attachment saved = repository.save(
                new Attachment(currentUser.id(), safeName(file.getOriginalFilename()), sniff(data), data));
        return meta(saved);
    }

    public AttachmentResponse findMeta(UUID id) {
        return repository.findMeta(id, currentUser.id())
                .orElseThrow(() -> new ResourceNotFoundException(id));
    }

    /** 바이트까지 필요한 다운로드·미리보기 경로. */
    public Attachment findOwned(UUID id) {
        return repository.findByIdAndOwnerId(id, currentUser.id())
                .orElseThrow(() -> new ResourceNotFoundException(id));
    }

    @Transactional
    public void delete(UUID id) {
        repository.delete(findOwned(id));
    }

    /**
     * 파일 앞머리로 타입을 정한다. 확장자와 Content-Type 헤더는 클라이언트가 하는 말이라 믿지 않는다 —
     * 이 값이 그대로 다운로드 응답의 Content-Type 이 되기 때문이다.
     */
    static String sniff(byte[] data) {
        if (startsWith(data, PNG)) return "image/png";
        if (startsWith(data, JPEG)) return "image/jpeg";
        if (startsWith(data, PDF)) return "application/pdf";
        throw new ApiRuleException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_FILE_TYPE",
                "jpg, png, pdf 만 첨부할 수 있습니다.");
    }

    /** 표시용 이름. 경로 구분자를 떼고 자른다 — 어떤 경로 조립에도 쓰지 않는다. */
    static String safeName(String raw) {
        if (raw == null || raw.isBlank()) return "첨부파일";
        String name = raw.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).trim();
        if (name.isEmpty()) return "첨부파일";
        return name.length() > 260 ? name.substring(0, 260) : name;
    }

    private static boolean startsWith(byte[] data, byte[] magic) {
        if (data.length < magic.length) return false;
        for (int i = 0; i < magic.length; i++) {
            if (data[i] != magic[i]) return false;
        }
        return true;
    }

    private static byte[] read(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static AttachmentResponse meta(Attachment attachment) {
        return new AttachmentResponse(attachment.getId(), attachment.getFilename(),
                attachment.getContentType(), attachment.getSizeBytes());
    }
}
