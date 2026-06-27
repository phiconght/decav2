package com.trungtam.schedule.controller;

import com.trungtam.common.dto.ApiResponse;
import com.trungtam.schedule.dto.request.CancelSessionRequest;
import com.trungtam.schedule.dto.request.CheckinRequest;
import com.trungtam.schedule.dto.request.CreateManualSessionRequest;
import com.trungtam.schedule.dto.request.CreateScheduleRequest;
import com.trungtam.schedule.dto.request.TimetableQuery;
import com.trungtam.schedule.dto.request.UpdateAttendanceRequest;
import com.trungtam.schedule.dto.request.UpdateSessionRequest;
import com.trungtam.schedule.dto.response.AttendanceItem;
import com.trungtam.schedule.dto.response.GeneratePreview;
import com.trungtam.schedule.dto.response.QrTokenResponse;
import com.trungtam.schedule.dto.response.ScheduleItem;
import com.trungtam.schedule.dto.response.SessionDetail;
import com.trungtam.schedule.dto.response.TimetableItem;
import com.trungtam.schedule.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * API Lich hoc, Buoi hoc, Diem danh, Timetable (§3.7).
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    // ---------------------- Quy tac lich ----------------------

    @GetMapping("/classes/{classId}/schedules")
    @PreAuthorize("hasAuthority('CLASS:READ')")
    public ApiResponse<List<ScheduleItem>> listSchedules(@PathVariable Long classId) {
        return ApiResponse.ok(scheduleService.listSchedules(classId));
    }

    @PostMapping("/classes/{classId}/schedules")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('CLASS:WRITE')")
    public ApiResponse<GeneratePreview> createSchedule(
            @PathVariable Long classId,
            @Valid @RequestBody CreateScheduleRequest request) {
        return ApiResponse.ok(scheduleService.createSchedule(classId, request));
    }

    @PostMapping("/classes/{classId}/schedules/preview")
    @PreAuthorize("hasAuthority('CLASS:WRITE')")
    public ApiResponse<GeneratePreview> previewSchedule(
            @PathVariable Long classId,
            @Valid @RequestBody CreateScheduleRequest request) {
        return ApiResponse.ok(scheduleService.previewSchedule(classId, request));
    }

    @PutMapping("/schedules/{id}")
    @PreAuthorize("hasAuthority('CLASS:WRITE')")
    public ApiResponse<GeneratePreview> updateSchedule(
            @PathVariable Long id,
            @Valid @RequestBody CreateScheduleRequest request) {
        return ApiResponse.ok(scheduleService.updateSchedule(id, request));
    }

    @DeleteMapping("/schedules/{id}")
    @PreAuthorize("hasAuthority('CLASS:WRITE')")
    public ApiResponse<Void> deleteSchedule(@PathVariable Long id) {
        scheduleService.deleteSchedule(id);
        return ApiResponse.ok();
    }

    // ---------------------- Buoi hoc ----------------------

    @GetMapping("/classes/{classId}/sessions")
    @PreAuthorize("hasAuthority('CLASS:READ')")
    public ApiResponse<List<SessionDetail>> listSessions(
            @PathVariable Long classId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(scheduleService.listSessions(classId, from, to));
    }

    @PostMapping("/classes/{classId}/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('CLASS:WRITE')")
    public ApiResponse<SessionDetail> createManualSession(
            @PathVariable Long classId,
            @Valid @RequestBody CreateManualSessionRequest request) {
        return ApiResponse.ok(scheduleService.createManualSession(classId, request));
    }

    @PatchMapping("/sessions/{id}")
    @PreAuthorize("hasAuthority('CLASS:WRITE')")
    public ApiResponse<SessionDetail> updateSession(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSessionRequest request) {
        return ApiResponse.ok(scheduleService.updateSession(id, request));
    }

    @PostMapping("/sessions/{id}/cancel")
    @PreAuthorize("hasAuthority('CLASS:WRITE')")
    public ApiResponse<SessionDetail> cancelSession(
            @PathVariable Long id,
            @Valid @RequestBody CancelSessionRequest request) {
        return ApiResponse.ok(scheduleService.cancelSession(id, request.reason()));
    }

    // ---------------------- QR token + check-in/out ----------------------

    @GetMapping("/sessions/{id}/qr-token")
    @PreAuthorize("hasAuthority('CLASS:READ')")
    public ApiResponse<QrTokenResponse> getQrToken(@PathVariable Long id) {
        return ApiResponse.ok(scheduleService.getQrToken(id));
    }

    @PostMapping("/sessions/{id}/checkin")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> checkin(
            @PathVariable Long id,
            @Valid @RequestBody CheckinRequest request) {
        scheduleService.checkinSelf(id, request.token());
        return ApiResponse.ok();
    }

    @PostMapping("/sessions/{id}/checkout")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> checkout(
            @PathVariable Long id,
            @Valid @RequestBody CheckinRequest request) {
        scheduleService.checkoutSelf(id, request.token());
        return ApiResponse.ok();
    }

    // ---------------------- Diem danh ----------------------

    @GetMapping("/sessions/{id}/attendance")
    @PreAuthorize("hasAuthority('CLASS:READ')")
    public ApiResponse<List<AttendanceItem>> listAttendance(@PathVariable Long id) {
        return ApiResponse.ok(scheduleService.listAttendance(id));
    }

    @PatchMapping("/sessions/{id}/attendance/{userId}")
    @PreAuthorize("hasAuthority('CLASS:WRITE')")
    public ApiResponse<Void> setAttendance(
            @PathVariable Long id,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateAttendanceRequest request) {
        scheduleService.setAttendance(id, userId, request.status());
        return ApiResponse.ok();
    }

    // ---------------------- Timetable ----------------------

    @GetMapping("/timetable")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<TimetableItem>> timetable(@Validated @ModelAttribute TimetableQuery query) {
        return ApiResponse.ok(scheduleService.timetable(query));
    }
}
