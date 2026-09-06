package com.jobsight.company.companycontent;

import com.jobsight.company.common.ApiPaths;
import com.jobsight.company.companycontent.dto.CompanyContentRequest;
import com.jobsight.company.companycontent.dto.CompanyContentResponse;
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

@Tag(name = "company contents", description = "기업별 뉴스·유튜브 자료 CRUD. 인증 필수.")
@RestController
@RequestMapping(ApiPaths.COMPANY_CONTENTS)
public class CompanyContentController {
    private final CompanyContentService service;

    public CompanyContentController(CompanyContentService service) {
        this.service = service;
    }

    @Operation(summary = "기업 뉴스·유튜브 목록", description = "최근 수정 순. 타인 소유 기업은 404.")
    @GetMapping
    public List<CompanyContentResponse> findAll(@PathVariable UUID companyId) {
        return service.findAll(companyId);
    }

    @Operation(summary = "기업 뉴스·유튜브 추가")
    @PostMapping
    public ResponseEntity<CompanyContentResponse> create(@PathVariable UUID companyId,
                                                          @Valid @RequestBody CompanyContentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(companyId, request));
    }

    @Operation(summary = "기업 뉴스·유튜브 수정")
    @PutMapping("/{id}")
    public CompanyContentResponse update(@PathVariable UUID companyId, @PathVariable UUID id,
                                         @Valid @RequestBody CompanyContentRequest request) {
        return service.update(companyId, id, request);
    }

    @Operation(summary = "기업 뉴스·유튜브 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID companyId, @PathVariable UUID id) {
        service.delete(companyId, id);
        return ResponseEntity.noContent().build();
    }
}
