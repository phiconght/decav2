package com.trungtam.payment.controller;

import com.trungtam.common.dto.ApiResponse;
import com.trungtam.payment.dto.response.StudentSessionReport;
import com.trungtam.payment.service.PaymentExcelService;
import com.trungtam.payment.service.PaymentReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Bao cao buoi hoc HV/GV + xuat Excel (SPEC_ThanhToan §2.8). Tat ca gate FEE:READ.
 */
@RestController
@RequestMapping("/api/v1/admin/reports")
@RequiredArgsConstructor
public class PaymentReportController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final PaymentReportService reportService;
    private final PaymentExcelService excelService;

    @GetMapping("/student-sessions")
    @PreAuthorize("hasAuthority('FEE:READ')")
    public ApiResponse<StudentSessionReport> studentSessions(
            @RequestParam Long studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(reportService.studentSessions(studentId, from, to));
    }

    @GetMapping("/student-sessions/export")
    @PreAuthorize("hasAuthority('FEE:READ')")
    public ResponseEntity<byte[]> exportStudentSessions(
            @RequestParam Long studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        byte[] bytes = excelService.exportStudentSessions(studentId, from, to);
        String filename = "hocvien_" + studentId + "_" + from + "_" + to + ".xlsx";
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(bytes);
    }

    @GetMapping("/teacher-sessions/export")
    @PreAuthorize("hasAuthority('FEE:READ')")
    public ResponseEntity<byte[]> exportTeacherSessions(
            @RequestParam Long teacherId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        byte[] bytes = excelService.exportTeacherSessions(teacherId, from, to);
        String filename = "giaovien_" + teacherId + "_" + from + "_" + to + ".xlsx";
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(bytes);
    }
}
