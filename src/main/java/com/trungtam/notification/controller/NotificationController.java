package com.trungtam.notification.controller;

import com.trungtam.notification.dto.request.NotificationSearchParams;
import com.trungtam.notification.dto.response.NotificationPageResponse;
import com.trungtam.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lich su thong bao cua nguoi dung dang dang nhap (§5.4).
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public NotificationPageResponse listMine(@ModelAttribute NotificationSearchParams params) {
        return notificationService.listMine(params);
    }
}
