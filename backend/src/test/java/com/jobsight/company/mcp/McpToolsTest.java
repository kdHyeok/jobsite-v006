package com.jobsight.company.mcp;

import com.jobsight.company.admin.AdminController;
import com.jobsight.company.auth.*;
import com.jobsight.company.company.*;
import com.jobsight.company.company.dto.CompanyRequest;
import com.jobsight.company.companycontent.*;
import com.jobsight.company.position.*;
import com.jobsight.company.posting.*;
import com.jobsight.company.reference.*;
import com.jobsight.company.setting.AppSettingService;
import com.jobsight.company.user.AppUserService;
import jakarta.validation.Validation;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class McpToolsTest {
    CompanyService companies = mock(CompanyService.class);
    JobPostingService postings = mock(JobPostingService.class);
    PositionService positions = mock(PositionService.class);
    ReferenceService references = mock(ReferenceService.class);
    CompanyContentService contents = mock(CompanyContentService.class);
    AppUserService users = mock(AppUserService.class);
    AppSettingService settings = mock(AppSettingService.class);
    CurrentUser current = new CurrentUser();
    McpTools tools;
    McpPrincipal principal = new McpPrincipal(UUID.randomUUID(), Set.copyOf(McpOAuthConfig.SCOPES));

    @BeforeEach void setUp() {
        var validator = Validation.buildDefaultValidatorFactory().getValidator();
        tools = new McpTools(new CompanyController(companies), new JobPostingController(postings),
                new PositionController(positions), new ReferenceController(references), new CompanyContentController(contents),
                new AdminController(users, settings, current), new AuthController(users, settings, current, null),
                new McpActions(current, users, positions, references, contents), JsonMapper.builder().findAndAddModules().build(), validator);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void catalogScopesAndAdministratorRoleAreEnforced() {
        assertThat(tools.list(principal)).hasSize(31);
        assertThat(tools.list(new McpPrincipal(principal.userId(), Set.of(McpOAuthConfig.READ))))
                .allSatisfy(tool -> assertThat(((Map<?, ?>) tool.get("annotations")).get("readOnlyHint")).isEqualTo(true));
        assertThatThrownBy(() -> tools.call("admin_users_list", Map.of(), principal))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThatThrownBy(() -> tools.call("company_delete", Map.of("id", UUID.randomUUID().toString()),
                new McpPrincipal(principal.userId(), Set.of(McpOAuthConfig.READ))))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThat(current.id()).isEqualTo(principal.userId());
    }
    @Test void validDtoReachesExistingControllerAndInvalidDataNeverDoes() {
        tools.call("company_create", Map.of("request", Map.of("name", "테스트", "benefits", "유연근무",
                "annualRevenue", 2500000000L, "revenueUnit", "HUNDRED_MILLION")), principal);
        verify(companies).create(argThat(r -> r.name().equals("테스트") && r.annualRevenue() == 2500000000L));
        clearInvocations(companies);
        for (Map<String, Object> bad : List.<Map<String, Object>>of(Map.of("name", ""), Map.of("name", "테스트", "websiteUrl", "javascript:alert(1)"),
                Map.of("name", "테스트", "ownerId", UUID.randomUUID().toString()), Map.of("name", "테스트", "annualRevenue", 1.5),
                Map.of("name", "테스트", "employeeCount", -1), Map.of("name", "테스트", "industries", List.of("x".repeat(61))))) {
            assertThatThrownBy(() -> tools.call("company_create", Map.of("request", bad), principal)).isInstanceOf(RuntimeException.class);
        }
        verifyNoInteractions(companies);
    }
    @Test void defaultsAndRelationshipArgumentsArePreserved() {
        tools.call("posting_list", Map.of(), principal);
        verify(postings).findOpen();
        tools.call("posting_list", Map.of("archived", true), principal);
        verify(postings).findArchived();
        UUID id = UUID.randomUUID(), reference = UUID.randomUUID();
        tools.call("position_replace_references", Map.of("id", id.toString(), "request", Map.of("referenceIds", List.of(reference.toString()))), principal);
        verify(positions).replaceReferences(eq(id), argThat(r -> r.referenceIds().equals(List.of(reference))));
        tools.call("company_content_delete", Map.of("companyId", id.toString(), "id", reference.toString()), principal);
        verify(contents).delete(id, reference);
    }
    @Test void schemaContainsWholeCompanyDtoAndStatusEnums() {
        var company = tools.list(principal).stream().filter(t -> t.get("name").equals("company_create")).findFirst().orElseThrow();
        String schema = JsonMapper.builder().build().writeValueAsString(company.get("inputSchema"));
        for (var field : CompanyRequest.class.getRecordComponents()) assertThat(schema).contains("\"" + field.getName() + "\"");
        var status = tools.list(principal).stream().filter(t -> t.get("name").equals("posting_set_status")).findFirst().orElseThrow();
        String statusSchema = JsonMapper.builder().build().writeValueAsString(status.get("inputSchema"));
        for (var value : ApplicationStatus.values()) assertThat(statusSchema).contains(value.name());
    }
}
