package com.trungtam.message.controller;

import com.trungtam.common.dto.ApiResponse;
import com.trungtam.message.dto.request.MessageSearchParams;
import com.trungtam.message.dto.response.MessageDetail;
import com.trungtam.message.dto.response.MessagePageResponse;
import com.trungtam.message.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Hop thu tin nhan (noi dung day du) cua nguoi dung dang dang nhap.
 */
@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public MessagePageResponse listMine(@ModelAttribute MessageSearchParams params) {
        return messageService.listMine(params);
    }

    @GetMapping("/me/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Long>> unreadCount() {
        return ApiResponse.ok(Map.of("count", messageService.unreadCount()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<MessageDetail> detail(@PathVariable Long id) {
        return ApiResponse.ok(messageService.detail(id));
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> markRead(@PathVariable Long id) {
        messageService.markRead(id);
        return ApiResponse.ok();
    }

    @PatchMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> markAllRead() {
        messageService.markAllRead();
        return ApiResponse.ok();
    }
}
