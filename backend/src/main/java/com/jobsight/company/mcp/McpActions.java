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
import com.jobsight.company.resume.ResumeContent;
import com.jobsight.company.resume.ResumeSection;
import com.jobsight.company.resume.ResumeService;
import com.jobsight.company.resume.dto.ResumeResponse;
import com.jobsight.company.resume.dto.ResumeRowRequest;
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
    private final ResumeService resumes;
    public McpActions(CurrentUser current, AppUserService users, PositionService positions,
                      ReferenceService references, CompanyContentService contents, ResumeService resumes) {
        this.current = current; this.users = users; this.positions = positions;
        this.references = references; this.contents = contents; this.resumes = resumes;
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

    @Operation(summary = "이력서 기본정보 변경",
            description = "basic 블록(성명·연락처·주소·링크)만 교체한다. 다른 섹션은 그대로다.")
    public ResumeResponse updateResumeBasic(@PathVariable UUID id, @Valid @RequestBody ResumeContent.BasicInfo request) {
        return resumes.updateBasic(id, request);
    }

    @Operation(summary = "이력서 섹션에 행 추가",
            description = "section 은 educations/trainings/activities/experiences/awards/certificates/skills/projects. "
                    + "request 에는 그 섹션의 필드만 넣는다(다른 섹션 필드는 UNKNOWN_ROW_FIELD). 맨 뒤에 붙는다.")
    public ResumeResponse addResumeRow(@PathVariable UUID id, @PathVariable ResumeSection section,
                                       @Valid @RequestBody ResumeRowRequest request) {
        return resumes.addRow(id, section, request);
    }

    @Operation(summary = "이력서 섹션 행 교체",
            description = "index 는 0부터. 그 행 전체를 request 로 바꾼다 — 먼저 resume_get 으로 읽고 유지할 필드를 함께 보낸다.")
    public ResumeResponse updateResumeRow(@PathVariable UUID id, @PathVariable ResumeSection section,
                                          @PathVariable int index, @Valid @RequestBody ResumeRowRequest request) {
        return resumes.updateRow(id, section, index, request);
    }

    @Operation(summary = "이력서 섹션 행 삭제",
            description = "index 는 0부터. 뒤 행이 앞으로 당겨지므로 여러 행을 지울 때는 큰 index 부터.")
    public ResumeResponse deleteResumeRow(@PathVariable UUID id, @PathVariable ResumeSection section,
                                          @PathVariable int index) {
        return resumes.deleteRow(id, section, index);
    }

    @Operation(summary = "기업 뉴스·유튜브 상세 조회")
    public CompanyContentResponse getContent(@PathVariable UUID companyId, @PathVariable UUID id) {
        return contents.findAll(companyId).stream().filter(value -> value.id().equals(id)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(id));
    }
}
