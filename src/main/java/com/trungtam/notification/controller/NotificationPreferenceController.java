package com.trungtam.notification.controller;

import com.trungtam.common.dto.ApiResponse;
import com.trungtam.notification.dto.request.UpdatePreferencesRequest;
import com.trungtam.notification.dto.response.PreferenceItem;
import com.trungtam.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Xem / sua tuy chon nhan thong bao (opt-in, gio im lang) cua nguoi dung hien tai (§5.4).
 */
@RestController
@RequestMapping("/api/v1/notification-preferences")
@RequiredArgsConstructor
public class NotificationPreferenceController {

    private final NotificationService notificationService;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<PreferenceItem>> listMine() {
        return ApiResponse.ok(notificationService.listMyPreferences());
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<PreferenceItem>> updateMine(
            @Valid @RequestBody UpdatePreferencesRequest request) {
        return ApiResponse.ok(notificationService.updateMyPreferences(request));
    }
}
