package com.trungtam.announcement.controller;

import com.trungtam.announcement.dto.request.CreateAnnouncementRequest;
import com.trungtam.announcement.dto.request.PreviewAudienceRequest;
import com.trungtam.announcement.dto.response.AnnouncementPageResponse;
import com.trungtam.announcement.dto.response.AnnouncementResult;
import com.trungtam.announcement.service.AnnouncementService;
import com.trungtam.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Soan / gui thong bao + xem lich su (ADMIN / EMPLOYEE).
 */
@RestController
@RequestMapping("/api/v1/admin/announcements")
@RequiredArgsConstructor
public class AdminAnnouncementController {

    private final AnnouncementService announcementService;

    @PostMapping
    @PreAuthorize("hasAuthority('ANNOUNCE:WRITE')")
    public ApiResponse<AnnouncementResult> send(
            @Valid @RequestBody CreateAnnouncementRequest req) {
        return ApiResponse.ok(announcementService.send(req));
    }

    @PostMapping("/preview-count")
    @PreAuthorize("hasAuthority('ANNOUNCE:WRITE')")
    public ApiResponse<Map<String, Long>> previewCount(
            @Valid @RequestBody PreviewAudienceRequest req) {
        return ApiResponse.ok(Map.of("count", announcementService.previewCount(req)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ANNOUNCE:READ')")
    public AnnouncementPageResponse history(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int pageSize) {
        return announcementService.history(current, pageSize);
    }
}
