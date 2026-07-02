package com.trungtam.report.controller;

import com.trungtam.common.dto.ApiResponse;
import com.trungtam.report.dto.response.BreakdownResponse;
import com.trungtam.report.dto.response.ClassAttendanceReport;
import com.trungtam.report.dto.response.ClassExamAverageItem;
import com.trungtam.report.dto.response.ClassStudentAverageItem;
import com.trungtam.report.dto.response.TopicMasteryItem;
import com.trungtam.report.service.ClassReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Bao cao cap LOP. Scope: canAccessClass (TEACHER/ASSISTANT lop minh, ADMIN/EMPLOYEE tat ca).
 */
@RestController
@RequestMapping("/api/v1/reports/classes")
@RequiredArgsConstructor
public class ClassReportController {

    private static final String CAN_ACCESS =
            "hasAuthority('REPORT:READ') and @securityService.canAccessClass(#classId, authentication)";

    private final ClassReportService classReportService;

    @GetMapping("/{classId}/exam-averages")
    @PreAuthorize(CAN_ACCESS)
    public ApiResponse<List<ClassExamAverageItem>> examAverages(@PathVariable Long classId) {
        return ApiResponse.ok(classReportService.examAverages(classId));
    }

    @GetMapping("/{classId}/breakdowns")
    @PreAuthorize(CAN_ACCESS)
    public ApiResponse<BreakdownResponse> breakdowns(@PathVariable Long classId) {
        return ApiResponse.ok(classReportService.breakdowns(classId));
    }

    @GetMapping("/{classId}/topic-mastery")
    @PreAuthorize(CAN_ACCESS)
    public ApiResponse<List<TopicMasteryItem>> topicMastery(@PathVariable Long classId) {
        return ApiResponse.ok(classReportService.topicMastery(classId));
    }

    @GetMapping("/{classId}/attendance")
    @PreAuthorize(CAN_ACCESS)
    public ApiResponse<ClassAttendanceReport> attendance(@PathVariable Long classId) {
        return ApiResponse.ok(classReportService.attendance(classId));
    }

    @GetMapping("/{classId}/students")
    @PreAuthorize(CAN_ACCESS)
    public ApiResponse<List<ClassStudentAverageItem>> students(@PathVariable Long classId) {
        return ApiResponse.ok(classReportService.students(classId));
    }
}
