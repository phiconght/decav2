package com.trungtam.notification.controller;

import com.trungtam.common.dto.ApiResponse;
import com.trungtam.notification.dto.request.RegisterDeviceRequest;
import com.trungtam.notification.dto.request.UnregisterDeviceRequest;
import com.trungtam.notification.service.DeviceTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dang ky / huy dang ky token thiet bi cua nguoi dung dang dang nhap (§5.4).
 */
@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceTokenService deviceTokenService;

    @PostMapping("/register")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterDeviceRequest request) {
        deviceTokenService.register(request);
        return ApiResponse.ok();
    }

    @PostMapping("/unregister")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> unregister(@Valid @RequestBody UnregisterDeviceRequest request) {
        deviceTokenService.unregister(request);
        return ApiResponse.ok();
    }
}
