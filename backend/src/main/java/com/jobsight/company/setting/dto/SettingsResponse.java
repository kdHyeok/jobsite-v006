package com.jobsight.company.setting.dto;

import com.jobsight.company.setting.AppSetting;

import java.time.Instant;

public record SettingsResponse(boolean autoApproveSignup, Instant updatedAt) {
    public static SettingsResponse from(AppSetting setting) {
        return new SettingsResponse(setting.isAutoApproveSignup(), setting.getUpdatedAt());
    }
}
