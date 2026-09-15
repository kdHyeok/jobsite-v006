package com.jobsight.company.adminrequest;

import com.jobsight.company.adminrequest.dto.AdminRequestResponse;
import com.jobsight.company.adminrequest.dto.AdminRequestWriteRequest;
import com.jobsight.company.common.ApiPaths;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "admin requests", description = "현재 계정의 관리자 요청과 피드백 확인.")
@RestController
@RequestMapping(ApiPaths.REQUESTS)
public class AdminRequestController {
    private final AdminRequestService service;

    public AdminRequestController(AdminRequestService service) { this.service = service; }

    @Operation(summary = "내 관리자 요청 목록")
    @GetMapping
    public List<AdminRequestResponse> findMine() { return service.findMine(); }

    @Operation(summary = "관리자 요청 보내기", description = "계정별 5초에 한 번, 한국 시간 하루 50회.")
    @PostMapping
    public ResponseEntity<AdminRequestResponse> create(@Valid @RequestBody AdminRequestWriteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @Operation(summary = "내 관리자 요청 수정")
    @PutMapping("/{id}")
    public AdminRequestResponse update(@PathVariable UUID id, @Valid @RequestBody AdminRequestWriteRequest request) {
        return service.update(id, request);
    }

    @Operation(summary = "내 관리자 요청 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.deleteMine(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "관리자 피드백 확인")
    @PatchMapping("/{id}/feedback/read")
    public AdminRequestResponse readFeedback(@PathVariable UUID id) { return service.readFeedback(id); }
}
