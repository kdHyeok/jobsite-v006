package com.jobsight.company.setting;

import com.jobsight.company.setting.dto.SettingsResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AppSettingService {
    private final AppSettingRepository repository;

    public AppSettingService(AppSettingRepository repository) {
        this.repository = repository;
    }

    public boolean isAutoApproveSignup() {
        return load().isAutoApproveSignup();
    }

    public SettingsResponse find() {
        return SettingsResponse.from(load());
    }

    @Transactional
    public SettingsResponse updateAutoApproveSignup(boolean autoApproveSignup) {
        AppSetting setting = load();
        setting.changeAutoApproveSignup(autoApproveSignup);
        return SettingsResponse.from(repository.save(setting));
    }

    private AppSetting load() {
        return repository.findById(AppSetting.SINGLETON_ID).orElseThrow(() ->
                new IllegalStateException("app_settings 싱글턴 행(id=1)이 없습니다. V4 마이그레이션을 확인하세요."));
    }
}
