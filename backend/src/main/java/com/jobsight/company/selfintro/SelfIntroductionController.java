package com.jobsight.company.selfintro;

import com.jobsight.company.common.ApiPaths;
import com.jobsight.company.selfintro.dto.SelfIntroductionRequest;
import com.jobsight.company.selfintro.dto.SelfIntroductionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequestMapping(ApiPaths.SELF_INTRODUCTIONS)
@Tag(name = "self-introductions", description = "이력서별 자기소개 질문과 답변. 인증 필수.")
public class SelfIntroductionController {
    private final SelfIntroductionService service;

    public SelfIntroductionController(SelfIntroductionService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "자기소개 문항 검색", description = "질문·답변의 동일 단어를 BM25 점수순으로 찾는다.")
    public List<SelfIntroductionResponse> findAll(
            @RequestParam(required = false) @Size(max = 100) String q,
            @RequestParam(required = false) UUID resumeId) {
        return service.findAll(q, resumeId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "자기소개 문항 상세 조회")
    public SelfIntroductionResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    @Operation(summary = "자기소개 문항 추가")
    public ResponseEntity<SelfIntroductionResponse> create(@Valid @RequestBody SelfIntroductionRequest request) {
        SelfIntroductionResponse created = service.create(request);
        return ResponseEntity.created(URI.create(ApiPaths.SELF_INTRODUCTIONS + "/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "자기소개 문항 변경")
    public SelfIntroductionResponse update(@PathVariable UUID id,
                                           @Valid @RequestBody SelfIntroductionRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "자기소개 문항 삭제")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
