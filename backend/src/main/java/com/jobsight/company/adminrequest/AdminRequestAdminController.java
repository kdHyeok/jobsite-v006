package com.jobsight.company.adminrequest;

import com.jobsight.company.adminrequest.dto.AdminFeedbackRequest;
import com.jobsight.company.adminrequest.dto.AdminRequestAdminResponse;
import com.jobsight.company.common.ApiPaths;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "admin request management", description = "관리자 요청·피드백 관리. ADMIN 전용.")
@RestController
@RequestMapping(ApiPaths.ADMIN_REQUESTS)
public class AdminRequestAdminController {
    private final AdminRequestService service;

    public AdminRequestAdminController(AdminRequestService service) { this.service = service; }

    @Operation(summary = "전체 관리자 요청 목록")
    @GetMapping
    public List<AdminRequestAdminResponse> findAll() { return service.findAllForAdmin(); }

    @Operation(summary = "요청 피드백 작성·수정")
    @PutMapping("/{id}/feedback")
    public AdminRequestAdminResponse saveFeedback(@PathVariable UUID id,
                                                   @Valid @RequestBody AdminFeedbackRequest request) {
        return service.saveFeedback(id, request);
    }

    @Operation(summary = "요청 피드백 삭제")
    @DeleteMapping("/{id}/feedback")
    public AdminRequestAdminResponse deleteFeedback(@PathVariable UUID id) {
        return service.deleteFeedback(id);
    }

    @Operation(summary = "관리자 요청 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.deleteForAdmin(id);
        return ResponseEntity.noContent().build();
    }
}
