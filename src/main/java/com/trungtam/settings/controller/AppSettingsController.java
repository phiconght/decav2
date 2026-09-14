package com.trungtam.settings.controller;

import com.trungtam.common.dto.ApiResponse;
import com.trungtam.settings.dto.request.UpdateAppSettingsRequest;
import com.trungtam.settings.dto.response.AppSettingsResponse;
import com.trungtam.settings.service.AppSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cau hinh dung chung toan he thong (hotline...) — Web/Mobile tai 1 lan luc
 * mo app. {@code permitAll()}: khach chua dang nhap van can thay hotline.
 */
@RestController
@RequestMapping("/api/v1/app-settings")
@RequiredArgsConstructor
public class AppSettingsController {

    private final AppSettingsService settingsService;

    @GetMapping
    @PreAuthorize("permitAll()")
    public ApiResponse<AppSettingsResponse> get() {
        return ApiResponse.ok(settingsService.get());
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AppSettingsResponse> update(@Valid @RequestBody UpdateAppSettingsRequest request) {
        return ApiResponse.ok(settingsService.update(request));
    }
}
