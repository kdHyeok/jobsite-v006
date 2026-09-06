package com.jobsight.company.mcp;

import com.jobsight.company.auth.CurrentUser;
import com.jobsight.company.auth.dto.MeResponse;
import com.jobsight.company.common.ResourceNotFoundException;
import com.jobsight.company.companycontent.CompanyContentService;
import com.jobsight.company.companycontent.dto.CompanyContentResponse;
import com.jobsight.company.position.PositionService;
import com.jobsight.company.position.dto.PositionRequest;
import com.jobsight.company.position.dto.PositionResponse;
import com.jobsight.company.reference.ReferenceService;
import com.jobsight.company.reference.dto.ReferenceResponse;
import com.jobsight.company.user.AppUserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

/** Only the small operations that do not have an existing REST controller method. */
@Component
public class McpActions {
    private final CurrentUser current;
    private final AppUserService users;
    private final PositionService positions;
    private final ReferenceService references;
    private final CompanyContentService contents;
    public McpActions(CurrentUser current, AppUserService users, PositionService positions,
                      ReferenceService references, CompanyContentService contents) {
        this.current = current; this.users = users; this.positions = positions;
        this.references = references; this.contents = contents;
    }
    @Operation(summary = "연결된 내 계정 조회")
    public MeResponse me() { return MeResponse.of(users.findById(current.id()).orElseThrow()); }

    @Operation(summary = "기존 공고에 모집 직무 추가", description = "다른 직무를 유지하면서 상세 필드를 가진 직무를 추가한다. 공고당 최대 30개.")
    public PositionResponse createPosition(@PathVariable UUID postingId, @Valid @RequestBody PositionRequest request) {
        return positions.create(postingId, request);
    }

    @Operation(summary = "참고 정보 상세 조회")
    public ReferenceResponse getReference(@PathVariable UUID id) {
        return references.findAll(null).stream().filter(value -> value.id().equals(id)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(id));
    }

    @Operation(summary = "기업 뉴스·유튜브 상세 조회")
    public CompanyContentResponse getContent(@PathVariable UUID companyId, @PathVariable UUID id) {
        return contents.findAll(companyId).stream().filter(value -> value.id().equals(id)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(id));
    }
}
