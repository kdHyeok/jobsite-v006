package com.jobsight.company.company;

import com.jobsight.company.common.ApiPaths;
import com.jobsight.company.company.dto.CompanyCreateRequest;
import com.jobsight.company.company.dto.CompanyResponse;
import com.jobsight.company.company.dto.CompanyUpdateRequest;
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

/** 모든 응답은 로그인 계정이 소유한 기업으로 한정된다. 타인 소유 id 는 404. */
@Tag(name = "companies", description = "로그인 계정 소유의 기업 정보 CRUD. 인증 필수.")
@RestController
@RequestMapping(ApiPaths.COMPANIES)
public class CompanyController {
    private final CompanyService service;

    public CompanyController(CompanyService service) {
        this.service = service;
    }

    @Operation(summary = "내 기업 목록", description = "최근 수정 순.")
    @GetMapping
    public List<CompanyResponse> findAll() {
        return service.findAll();
    }

    @Operation(summary = "기업 상세", description = "타인 소유이거나 없으면 404.")
    @GetMapping("/{id}")
    public CompanyResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @Operation(summary = "기업 생성", description = "소유자는 현재 로그인 계정으로 고정된다.")
    @PostMapping
    public ResponseEntity<CompanyResponse> create(@Valid @RequestBody CompanyCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @Operation(summary = "기업 수정", description = "타인 소유이거나 없으면 404.")
    @PutMapping("/{id}")
    public CompanyResponse update(@PathVariable UUID id, @Valid @RequestBody CompanyUpdateRequest request) {
        return service.update(id, request);
    }

    @Operation(summary = "기업 삭제", description = "타인 소유이거나 없으면 404.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
