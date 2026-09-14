package com.trungtam.schoolclass.controller;

import com.trungtam.common.dto.ApiResponse;
import com.trungtam.schoolclass.dto.response.EnrollmentRequestItem;
import com.trungtam.schoolclass.entity.EnrollmentRequestStatus;
import com.trungtam.schoolclass.service.ClassRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Man Admin "Yêu cầu đăng ký khóa học" — doi chieu chuyen khoan thu cong roi
 * xac nhan + ghi danh (xem ClassRegistrationService).
 */
@RestController
@RequestMapping("/api/v1/admin/enrollment-requests")
@RequiredArgsConstructor
public class EnrollmentRequestController {

    private final ClassRegistrationService registrationService;

    @GetMapping
    @PreAuthorize("hasAuthority('CLASS:WRITE')")
    public ApiResponse<List<EnrollmentRequestItem>> list(
            @RequestParam(required = false) EnrollmentRequestStatus status) {
        return ApiResponse.ok(registrationService.adminList(status));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('CLASS:WRITE')")
    public ApiResponse<Void> confirm(@PathVariable Long id) {
        registrationService.confirm(id);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('CLASS:WRITE')")
    public ApiResponse<Void> cancel(@PathVariable Long id) {
        registrationService.cancel(id);
        return ApiResponse.ok();
    }
}
