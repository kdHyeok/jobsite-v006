package com.jobsight.company.adminrequest;

import com.jobsight.company.adminrequest.dto.*;
import com.jobsight.company.auth.CurrentUser;
import com.jobsight.company.common.ApiRuleException;
import com.jobsight.company.common.ResourceNotFoundException;
import com.jobsight.company.user.AppUser;
import com.jobsight.company.user.AppUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AdminRequestService {
    private static final ZoneId QUOTA_ZONE = ZoneId.of("Asia/Seoul");
    private final AdminRequestRepository requests;
    private final AppUserRepository users;
    private final CurrentUser currentUser;
    private final Clock clock;

    @Autowired
    public AdminRequestService(AdminRequestRepository requests, AppUserRepository users, CurrentUser currentUser) {
        this(requests, users, currentUser, Clock.systemUTC());
    }

    AdminRequestService(AdminRequestRepository requests, AppUserRepository users, CurrentUser currentUser, Clock clock) {
        this.requests = requests;
        this.users = users;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    public List<AdminRequestResponse> findMine() {
        return requests.findAllByOwnerIdOrderByUpdatedAtDesc(currentUser.id()).stream()
                .map(AdminRequestResponse::of).toList();
    }

    @Transactional
    public AdminRequestResponse create(AdminRequestWriteRequest input) {
        UUID ownerId = currentUser.id();
        AppUser user = users.findByIdForUpdate(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException(ownerId));
        Instant now = clock.instant();
        LocalDate today = LocalDate.ofInstant(now, QUOTA_ZONE);
        if (user.getLastAdminRequestAt() != null && now.isBefore(user.getLastAdminRequestAt().plusSeconds(5))) {
            throw new ApiRuleException(HttpStatus.TOO_MANY_REQUESTS, "REQUEST_TOO_FAST",
                    "요청은 5초에 한 번 보낼 수 있습니다.");
        }
        if (today.equals(user.getAdminRequestCountDate()) && user.getAdminRequestCount() >= 50) {
            throw new ApiRuleException(HttpStatus.TOO_MANY_REQUESTS, "REQUEST_DAILY_LIMIT",
                    "오늘 보낼 수 있는 관리자 요청 50회를 모두 사용했습니다.");
        }
        user.recordAdminRequest(today, now);
        return AdminRequestResponse.of(requests.save(
                new AdminRequest(ownerId, input.kind(), input.message(), now)));
    }

    @Transactional
    public AdminRequestResponse update(UUID id, AdminRequestWriteRequest input) {
        AdminRequest request = findOwned(id);
        request.update(input.kind(), input.message(), clock.instant());
        return AdminRequestResponse.of(requests.save(request));
    }

    @Transactional
    public void deleteMine(UUID id) {
        requests.delete(findOwned(id));
    }

    @Transactional
    public AdminRequestResponse readFeedback(UUID id) {
        AdminRequest request = findOwned(id);
        request.markFeedbackRead(clock.instant());
        return AdminRequestResponse.of(requests.save(request));
    }

    public List<AdminRequestAdminResponse> findAllForAdmin() {
        List<AdminRequest> all = requests.findAllByOrderByCreatedAtDesc();
        Map<UUID, AppUser> byId = users.findAllById(all.stream().map(AdminRequest::getOwnerId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(AppUser::getId, Function.identity()));
        return all.stream().map(request -> AdminRequestAdminResponse.of(request, byId.get(request.getOwnerId()))).toList();
    }

    @Transactional
    public AdminRequestAdminResponse saveFeedback(UUID id, AdminFeedbackRequest input) {
        AdminRequest request = findAny(id);
        request.saveFeedback(input.feedback(), clock.instant());
        return adminResponse(requests.save(request));
    }

    @Transactional
    public AdminRequestAdminResponse deleteFeedback(UUID id) {
        AdminRequest request = findAny(id);
        request.deleteFeedback(clock.instant());
        return adminResponse(requests.save(request));
    }

    @Transactional
    public void deleteForAdmin(UUID id) {
        requests.delete(findAny(id));
    }

    private AdminRequest findOwned(UUID id) {
        return requests.findByIdAndOwnerId(id, currentUser.id())
                .orElseThrow(() -> new ResourceNotFoundException(id));
    }

    private AdminRequest findAny(UUID id) {
        return requests.findById(id).orElseThrow(() -> new ResourceNotFoundException(id));
    }

    private AdminRequestAdminResponse adminResponse(AdminRequest request) {
        AppUser user = users.findById(request.getOwnerId())
                .orElseThrow(() -> new ResourceNotFoundException(request.getOwnerId()));
        return AdminRequestAdminResponse.of(request, user);
    }
}
