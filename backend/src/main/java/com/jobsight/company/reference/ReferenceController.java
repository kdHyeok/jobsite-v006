package com.jobsight.company.reference;

import com.jobsight.company.common.ApiPaths;
import com.jobsight.company.reference.dto.ReferenceRequest;
import com.jobsight.company.reference.dto.ReferenceResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "references", description = "참고 정보. 계정 안에서 공유되어 여러 직무에 붙는다. 인증 필수.")
@RestController
@RequestMapping(ApiPaths.REFERENCES)
public class ReferenceController {
    private final ReferenceService service;

    public ReferenceController(ReferenceService service) {
        this.service = service;
    }

    @Operation(summary = "참고 정보 목록", description = "q 로 제목·메모 부분 일치 검색.")
    @GetMapping
    public List<ReferenceResponse> findAll(@RequestParam(name = "q", required = false) String q) {
        return service.findAll(q);
    }

    @Operation(summary = "참고 정보 생성", description = "kind=RELATED_POSITION 이면 relatedPositionId 로 내 직무를 가리킨다.")
    @PostMapping
    public ResponseEntity<ReferenceResponse> create(@Valid @RequestBody ReferenceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @Operation(summary = "참고 정보 수정")
    @PutMapping("/{id}")
    public ReferenceResponse update(@PathVariable UUID id, @Valid @RequestBody ReferenceRequest request) {
        return service.update(id, request);
    }

    @Operation(summary = "참고 정보 삭제", description = "붙어 있던 모든 직무에서 함께 떨어진다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
