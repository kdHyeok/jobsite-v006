package com.jobsight.company.attachment;

import com.jobsight.company.common.ApiRuleException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AttachmentServiceTest {

    /** 타입은 확장자가 아니라 앞머리 바이트로 정해진다 — 이 값이 그대로 다운로드 Content-Type 이 된다. */
    @Test
    void sniffsTypeFromTheFileHeader() {
        assertThat(AttachmentService.sniff(new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0}))
                .isEqualTo("image/png");
        assertThat(AttachmentService.sniff(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0}))
                .isEqualTo("image/jpeg");
        assertThat(AttachmentService.sniff("%PDF-1.7\n".getBytes(StandardCharsets.UTF_8)))
                .isEqualTo("application/pdf");
    }

    /** png 로 이름만 바꾼 스크립트 파일은 415 다. 통과시키면 그 타입 그대로 다시 내려주게 된다. */
    @Test
    void rejectsAnythingElse() {
        assertThatThrownBy(() -> AttachmentService.sniff("<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(ApiRuleException.class)
                .hasMessageContaining("jpg, png, pdf");
        assertThatThrownBy(() -> AttachmentService.sniff(new byte[]{1}))
                .isInstanceOf(ApiRuleException.class);
    }

    /** 파일명은 표시용이다. 경로가 섞여 들어와도 마지막 조각만 남긴다. */
    @Test
    void keepsOnlyTheDisplayNameOfTheUpload() {
        assertThat(AttachmentService.safeName("C:\\Users\\me\\정보처리기사.pdf")).isEqualTo("정보처리기사.pdf");
        assertThat(AttachmentService.safeName("../../etc/passwd")).isEqualTo("passwd");
        assertThat(AttachmentService.safeName("   ")).isEqualTo("첨부파일");
        assertThat(AttachmentService.safeName(null)).isEqualTo("첨부파일");
        assertThat(AttachmentService.safeName("a".repeat(300))).hasSize(260);
    }
}
