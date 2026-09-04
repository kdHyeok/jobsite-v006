package com.jobsight.company.user;

import com.jobsight.company.common.ApiRuleException;
import com.jobsight.company.common.ResourceNotFoundException;
import com.jobsight.company.setting.AppSettingService;
import com.jobsight.company.user.dto.UserResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AppUserService {
    private final AppUserRepository repository;
    private final AppSettingService settings;
    private final String adminEmail;

    public AppUserService(AppUserRepository repository, AppSettingService settings,
                          @Value("${app.admin-email:}") String adminEmail) {
        this.repository = repository;
        this.settings = settings;
        this.adminEmail = adminEmail.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Google 로그인으로 들어온 신원을 계정으로 해석한다. 없으면 가입 신청 계정을 만든다.
     *
     * 이 메서드는 계정 상태를 검사하지 않는다. 호출자가 트랜잭션 밖에서 검사해야
     * 최초 로그인으로 만들어진 PENDING 계정이 "승인 대기" 예외 때문에 롤백되지 않는다.
     */
    @Transactional
    public AppUser resolveGoogleUser(String googleSub, String email, boolean emailVerified, String googleName) {
        String name = normalizeName(googleName);

        Optional<AppUser> bySub = repository.findByGoogleSub(googleSub);
        if (bySub.isPresent()) {
            return applyAdminRule(bySub.get());
        }

        // 이메일로 기존 계정을 찾아 연결하는 것은 Google 이 그 이메일을 검증했을 때만 안전하다.
        // 미검증 이메일을 신뢰하면 남의 계정을 가로챌 수 있다.
        if (!emailVerified || email == null || email.isBlank()) {
            throw new ApiRuleException(HttpStatus.FORBIDDEN, "EMAIL_NOT_VERIFIED",
                    "Google 계정의 이메일이 확인되지 않았습니다.");
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);

        Optional<AppUser> byEmail = repository.findByEmailIgnoreCase(normalized);
        if (byEmail.isPresent()) {
            AppUser existing = byEmail.get();
            existing.linkGoogle(googleSub, normalized, name);
            return applyAdminRule(repository.save(existing));
        }

        // 새 배포에서 관리자 이메일이 처음 로그인하면, 아직 비어 있는 부트스트랩 행을 넘겨받아
        // 시드 기업 데이터의 소유권을 그대로 이어받는다.
        if (isAdminEmail(normalized)) {
            Optional<AppUser> vacantSlot = repository.findById(AppUser.BOOTSTRAP_ADMIN_ID)
                    .filter(slot -> !slot.isGoogleLinked());
            if (vacantSlot.isPresent()) {
                AppUser slot = vacantSlot.get();
                slot.linkGoogle(googleSub, normalized, name);
                slot.promoteToAdmin();
                return repository.save(slot);
            }
        }

        // 가입을 막는 상태는 없다. 자동 승인이면 바로 ACTIVE, 아니면 PENDING 으로 접수되어 승인 대기열에 선다.
        UserStatus initialStatus = settings.isAutoApproveSignup() ? UserStatus.ACTIVE : UserStatus.PENDING;
        return applyAdminRule(repository.save(AppUser.signUpWithGoogle(normalized, googleSub, name, initialStatus)));
    }

    /**
     * APP_ADMIN_EMAIL 은 상시 규칙이다: 이 이메일의 계정은 로그인할 때마다 ADMIN/ACTIVE 로 보장된다.
     * 설정 파일이 권한의 최종 근거이므로, 화면에서 이 계정을 내리더라도 다음 로그인에 복구된다.
     */
    private AppUser applyAdminRule(AppUser user) {
        if (isAdminEmail(user.getEmail()) && !user.isActiveAdmin()) {
            user.promoteToAdmin();
            return repository.save(user);
        }
        return user;
    }

    private boolean isAdminEmail(String email) {
        return !adminEmail.isEmpty() && adminEmail.equals(email);
    }

    public Optional<AppUser> findById(UUID id) {
        return repository.findById(id);
    }

    public List<UserResponse> findAll() {
        return repository.findAllByOrderByCreatedAtDesc().stream()
                .map(UserResponse::from)
                .toList();
    }

    /** 본인과 관리자가 함께 쓴다. 빈 값은 이름 지우기. */
    @Transactional
    public AppUser changeDisplayName(UUID id, String displayName) {
        AppUser user = findEntity(id);
        user.changeDisplayName(normalizeName(displayName));
        return repository.save(user);
    }

    @Transactional
    public UserResponse changeStatus(UUID id, UserStatus status, UUID actingAdminId) {
        rejectSelfModification(id, actingAdminId, "상태");
        AppUser user = findEntity(id);
        // 마지막 활성 관리자를 잠그면 아무도 관리자 페이지에 들어갈 수 없게 된다.
        if (user.getRole() == UserRole.ADMIN && status != UserStatus.ACTIVE) {
            requireAnotherActiveAdmin(id);
        }
        user.changeStatus(status);
        return UserResponse.from(repository.save(user));
    }

    @Transactional
    public UserResponse changeRole(UUID id, UserRole role, UUID actingAdminId) {
        rejectSelfModification(id, actingAdminId, "권한");
        AppUser user = findEntity(id);
        if (user.getRole() == UserRole.ADMIN && role != UserRole.ADMIN) {
            requireAnotherActiveAdmin(id);
        }
        user.changeRole(role);
        return UserResponse.from(repository.save(user));
    }

    /**
     * 계정과 그 계정이 소유한 기업을 함께 지운다(companies.owner_id ON DELETE CASCADE).
     * 자기 자신과 마지막 활성 관리자는 지울 수 없다.
     */
    @Transactional
    public void deleteUser(UUID id, UUID actingAdminId) {
        rejectSelfModification(id, actingAdminId, "삭제");
        AppUser user = findEntity(id);
        if (user.isActiveAdmin()) {
            requireAnotherActiveAdmin(id);
        }
        repository.delete(user);
    }

    private void rejectSelfModification(UUID targetId, UUID actingAdminId, String what) {
        if (targetId.equals(actingAdminId)) {
            throw new ApiRuleException(HttpStatus.BAD_REQUEST, "SELF_MODIFICATION",
                    "자기 계정의 " + what + "은 여기서 할 수 없습니다.");
        }
    }

    private void requireAnotherActiveAdmin(UUID excludedId) {
        long others = repository.countByRoleAndStatusAndIdNot(
                UserRole.ADMIN, UserStatus.ACTIVE, excludedId);
        if (others == 0) {
            throw new ApiRuleException(HttpStatus.BAD_REQUEST, "LAST_ACTIVE_ADMIN",
                    "마지막 활성 관리자입니다. 다른 관리자를 먼저 지정해야 변경할 수 있습니다.");
        }
    }

    private AppUser findEntity(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(id));
    }

    private static String normalizeName(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
