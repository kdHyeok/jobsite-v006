package com.jobsight.company.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    @Query("select u from AppUser u where lower(u.email) = lower(:email)")
    Optional<AppUser> findByEmailIgnoreCase(@Param("email") String email);

    Optional<AppUser> findByGoogleSub(String googleSub);

    List<AppUser> findAllByOrderByCreatedAtDesc();

    long countByRoleAndStatusAndIdNot(UserRole role, UserStatus status, UUID id);
}
