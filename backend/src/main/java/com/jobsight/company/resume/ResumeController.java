package com.jobsight.company.resume;

import com.jobsight.company.common.ApiPaths;
import com.jobsight.company.resume.dto.ResumeCopyRequest;
import com.jobsight.company.resume.dto.ResumeRequest;
import com.jobsight.company.resume.dto.ResumeResponse;
import com.jobsight.company.resume.dto.ResumeSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** 모든 응답은 로그인 계정이 소유한 이력서로 한정된다. 타인 소유 id 는 404. */
@Tag(name = "resumes", description = "이력서. 이름+id 로 구분되는 여러 버전, 섹션은 JSON 문서 하나. 인증 필수.")
@RestController
@RequestMapping(ApiPaths.RESUMES)
public class ResumeController {
    private final ResumeService service;

    public ResumeController(ResumeService service) {
        this.service = service;
    }

    @Operation(summary = "이력서 목록", description = "최근 수정 순. content 는 싣지 않는다.")
    @GetMapping
    public List<ResumeSummary> findAll() {
        return service.findAll();
    }

    @Operation(summary = "이력서 상세", description = "content 전체. 타인 소유이거나 없으면 404.")
    @GetMapping("/{id}")
    public ResumeResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @Operation(summary = "이력서 생성", description = "content 를 비우면 빈 문서로 만든다.")
    @PostMapping
    public ResponseEntity<ResumeResponse> create(@Valid @RequestBody ResumeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @Operation(summary = "이력서 저장", description = "이름과 content 를 통째로 교체한다. 행 단위 API 는 없다.")
    @PutMapping("/{id}")
    public ResumeResponse update(@PathVariable UUID id, @Valid @RequestBody ResumeRequest request) {
        return service.update(id, request);
    }

    @Operation(summary = "이력서 복제", description = "같은 content 를 든 새 버전을 만든다. 원본은 그대로.")
    @PostMapping("/{id}/copy")
    public ResponseEntity<ResumeResponse> copy(@PathVariable UUID id, @Valid @RequestBody ResumeCopyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.copy(id, request));
    }

    @Operation(summary = "이력서 삭제", description = "타인 소유이거나 없으면 404.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
