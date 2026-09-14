package com.trungtam.settings.dto.response;

import com.trungtam.settings.entity.AppSettings;

public record AppSettingsResponse(String supportHotline) {
    public static AppSettingsResponse from(AppSettings s) {
        return new AppSettingsResponse(s.getSupportHotline());
    }
}
