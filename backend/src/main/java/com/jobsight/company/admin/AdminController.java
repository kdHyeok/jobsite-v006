package com.jobsight.company.admin;

import com.jobsight.company.admin.dto.UserDataCountResponse;
import com.jobsight.company.auth.CurrentUser;
import com.jobsight.company.common.ApiPaths;
import com.jobsight.company.setting.AppSettingService;
import com.jobsight.company.setting.dto.SettingsResponse;
import com.jobsight.company.setting.dto.SettingsUpdateRequest;
import com.jobsight.company.user.AppUserService;
import com.jobsight.company.user.dto.DisplayNameUpdateRequest;
import com.jobsight.company.user.dto.UserResponse;
import com.jobsight.company.user.dto.UserRoleUpdateRequest;
import com.jobsight.company.user.dto.UserStatusUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** 전체 경로가 SecurityConfig에서 hasRole('ADMIN')으로 보호된다. */
@Tag(name = "admin", description = "계정 승인·상태·권한·이름·삭제와 신규 가입 허용 설정. ADMIN 전용.")
@RestController
@RequestMapping(ApiPaths.ADMIN)
public class AdminController {
    private final AppUserService users;
    private final AppSettingService settings;
    private final AdminStatsService stats;
    private final CurrentUser currentUser;

    public AdminController(AppUserService users, AppSettingService settings, AdminStatsService stats,
                           CurrentUser currentUser) {
        this.users = users;
        this.settings = settings;
        this.stats = stats;
        this.currentUser = currentUser;
    }

    @Operation(summary = "전체 계정 목록", description = "최근 신청 순. googleLinked=false 는 로그인 수단이 없는 행이다.")
    @GetMapping("/users")
    public List<UserResponse> findUsers() {
        return users.findAll();
    }

    @Operation(summary = "계정별 등록 데이터 수",
            description = "계정이 등록한 기업·채용공고·모집 직무 개수. 개수만 돌려주고 내용은 담지 않는다. "
                    + "데이터가 하나도 없는 계정은 응답에서 빠진다.")
    @GetMapping("/users/counts")
    public List<UserDataCountResponse> findUserDataCounts() {
        return stats.countByUser();
    }

    @Operation(summary = "계정 상태 변경",
            description = "승인(ACTIVE)·거절(REJECTED)·정지(SUSPENDED)·대기 복귀(PENDING). 자기 계정과 마지막 활성 관리자는 거부(400).")
    @PatchMapping("/users/{id}/status")
    public UserResponse changeStatus(@PathVariable UUID id,
                                     @Valid @RequestBody UserStatusUpdateRequest request) {
        return users.changeStatus(id, request.status(), currentUser.id());
    }

    @Operation(summary = "계정 권한 변경", description = "USER/ADMIN. 자기 계정과 마지막 활성 관리자는 거부(400).")
    @PatchMapping("/users/{id}/role")
    public UserResponse changeRole(@PathVariable UUID id,
                                   @Valid @RequestBody UserRoleUpdateRequest request) {
        return users.changeRole(id, request.role(), currentUser.id());
    }

    @Operation(summary = "계정 표시 이름 변경", description = "빈 값이면 이름을 지운다. 이메일은 Google 신원이라 편집 대상이 아니다.")
    @PatchMapping("/users/{id}/name")
    public UserResponse changeName(@PathVariable UUID id,
                                   @Valid @RequestBody DisplayNameUpdateRequest request) {
        return UserResponse.from(users.changeDisplayName(id, request.displayName()));
    }

    @Operation(summary = "계정 삭제",
            description = "그 계정이 소유한 기업 정보도 함께 영구 삭제된다(CASCADE). 자기 계정과 마지막 활성 관리자는 거부(400).")
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        users.deleteUser(id, currentUser.id());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "전역 설정 조회")
    @GetMapping("/settings")
    public SettingsResponse findSettings() {
        return settings.find();
    }

    @Operation(summary = "신규 가입 자동 승인 여부 변경",
            description = "true 면 Google 첫 로그인이 곧 가입 완료(ACTIVE). false 면 PENDING 으로 접수되어 승인 후 이용. APP_ADMIN_EMAIL 은 항상 ACTIVE.")
    @PutMapping("/settings")
    public SettingsResponse updateSettings(@Valid @RequestBody SettingsUpdateRequest request) {
        return settings.updateAutoApproveSignup(request.autoApproveSignup());
    }
}
