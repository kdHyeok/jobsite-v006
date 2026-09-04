package com.jobsight.company.posting;

import com.jobsight.company.common.ApiPaths;
import com.jobsight.company.posting.dto.JobPostingRequest;
import com.jobsight.company.posting.dto.JobPostingResponse;
import com.jobsight.company.posting.dto.StageUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** 모든 응답은 로그인 계정이 소유한 공고로 한정된다. 타인 소유 id 는 404. */
@Tag(name = "postings", description = "채용공고 CRUD. 마감 임박 순 정렬, 마감된 관심 공고 자동 보관. 인증 필수.")
@RestController
@RequestMapping(ApiPaths.POSTINGS)
public class JobPostingController {
    private final JobPostingService service;

    public JobPostingController(JobPostingService service) {
        this.service = service;
    }

    @Operation(summary = "공고 목록",
            description = "기본은 진행 중 공고를 마감 임박 순(상시채용은 맨 뒤)으로. archived=true 면 보관함. "
                    + "조회 시점에 마감 지난 '관심' 공고를 보관함으로 옮긴다.")
    @GetMapping
    public List<JobPostingResponse> findAll(
            @RequestParam(name = "archived", defaultValue = "false") boolean archived) {
        return archived ? service.findArchived() : service.findOpen();
    }

    @Operation(summary = "공고 상세", description = "타인 소유이거나 없으면 404.")
    @GetMapping("/{id}")
    public JobPostingResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @Operation(summary = "공고 생성",
            description = "companyId 를 주면 그 기업에 연결되고 고용회사명은 기업 이름을 따른다. 남의 기업이면 404.")
    @PostMapping
    public ResponseEntity<JobPostingResponse> create(@Valid @RequestBody JobPostingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @Operation(summary = "공고 수정", description = "타인 소유이거나 없으면 404.")
    @PutMapping("/{id}")
    public JobPostingResponse update(@PathVariable UUID id,
                                     @Valid @RequestBody JobPostingRequest request) {
        return service.update(id, request);
    }

    @Operation(summary = "지원단계 변경",
            description = "관심 외 단계로 옮기면 보관함에서 자동으로 꺼낸다(지원 중인 공고가 보관함에 있으면 앞뒤가 맞지 않는다).")
    @PatchMapping("/{id}/stage")
    public JobPostingResponse changeStage(@PathVariable UUID id,
                                          @Valid @RequestBody StageUpdateRequest request) {
        return service.changeStage(id, request.stage());
    }

    @Operation(summary = "보관/복원", description = "archived=true 면 보관함으로, false 면 목록으로 되돌린다.")
    @PatchMapping("/{id}/archive")
    public JobPostingResponse setArchived(@PathVariable UUID id,
                                          @RequestParam(name = "archived", defaultValue = "true") boolean archived) {
        return service.setArchived(id, archived);
    }

    @Operation(summary = "공고 삭제", description = "타인 소유이거나 없으면 404.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
