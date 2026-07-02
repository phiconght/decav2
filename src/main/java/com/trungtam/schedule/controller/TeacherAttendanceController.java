package com.trungtam.schedule.controller;

import com.trungtam.common.dto.ApiResponse;
import com.trungtam.schedule.dto.request.SetTeacherAttendanceRequest;
import com.trungtam.schedule.dto.request.TeacherCheckinRequest;
import com.trungtam.schedule.dto.response.TeacherAttendanceView;
import com.trungtam.schedule.dto.response.TeacherWorkReport;
import com.trungtam.schedule.service.TeacherAttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * API cham cong day cua giao vien (QR phong) + bao cao cong.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TeacherAttendanceController {

    private final TeacherAttendanceService service;

    /** GV cham cong VAO — quet QR dan tai phong. */
    @PostMapping("/sessions/{id}/teacher-checkin")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<TeacherAttendanceView> checkin(
            @PathVariable Long id,
            @Valid @RequestBody TeacherCheckinRequest request) {
        return ApiResponse.ok(service.checkin(id, request.roomCode()));
    }

    /** GV cham cong RA — quet lai QR phong. */
    @PostMapping("/sessions/{id}/teacher-checkout")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<TeacherAttendanceView> checkout(
            @PathVariable Long id,
            @Valid @RequestBody TeacherCheckinRequest request) {
        return ApiResponse.ok(service.checkout(id, request.roomCode()));
    }

    /** Admin cham cong tay cho GV. */
    @PatchMapping("/sessions/{id}/teacher-attendance")
    @PreAuthorize("hasAuthority('CLASS:WRITE')")
    public ApiResponse<TeacherAttendanceView> setManual(
            @PathVariable Long id,
            @Valid @RequestBody SetTeacherAttendanceRequest request) {
        return ApiResponse.ok(service.setManual(id, request));
    }

    /** Trang thai cong 1 buoi (CHUA_CHAM neu chua co). */
    @GetMapping("/sessions/{id}/teacher-attendance")
    @PreAuthorize("hasAuthority('CLASS:READ')")
    public ApiResponse<TeacherAttendanceView> getStatus(@PathVariable Long id) {
        return ApiResponse.ok(service.getStatus(id));
    }

    /** GV tu xem cong cua minh trong khoang [from, to]. */
    @GetMapping("/teachers/me/teaching-attendance")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<TeacherWorkReport> myReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(service.myReport(from, to));
    }

    /** Admin/nhan vien xem cong cua 1 GV. */
    @GetMapping("/admin/teacher-attendance")
    @PreAuthorize("hasAuthority('CLASS:READ')")
    public ApiResponse<TeacherWorkReport> adminReport(
            @RequestParam Long teacherId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(service.adminReport(teacherId, from, to));
    }
}
