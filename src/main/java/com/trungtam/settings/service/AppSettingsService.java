package com.trungtam.settings.service;

import com.trungtam.settings.dto.request.UpdateAppSettingsRequest;
import com.trungtam.settings.dto.response.AppSettingsResponse;
import com.trungtam.settings.entity.AppSettings;
import com.trungtam.settings.repository.AppSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cau hinh dung chung toan he thong (single-row, id=1) — hotline hien tren
 * moi Card/trang chi tiet khoa hoc. Migration V50 da seed san 1 dong nen
 * khong can xu ly "chua cau hinh" nhu payment_settings.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AppSettingsService {

    private final AppSettingsRepository repository;

    public AppSettingsResponse get() {
        return AppSettingsResponse.from(repository.findById(AppSettings.SINGLETON_ID)
                .orElseGet(this::seedDefault));
    }

    @Transactional
    public AppSettingsResponse update(UpdateAppSettingsRequest req) {
        AppSettings s = repository.findById(AppSettings.SINGLETON_ID).orElseGet(this::seedDefault);
        s.setSupportHotline(req.supportHotline());
        return AppSettingsResponse.from(repository.save(s));
    }

    private AppSettings seedDefault() {
        AppSettings s = new AppSettings();
        s.setId(AppSettings.SINGLETON_ID);
        s.setSupportHotline("1900 9999");
        return s;
    }
}
