package com.trungtam.report.controller;

import com.trungtam.common.dto.ApiResponse;
import com.trungtam.report.dto.response.BreakdownResponse;
import com.trungtam.report.dto.response.ChildOption;
import com.trungtam.report.dto.response.ExamReportDetail;
import com.trungtam.report.dto.response.RecentExamItem;
import com.trungtam.report.dto.response.ScoreTrendPoint;
import com.trungtam.report.dto.response.StudentAttendanceReport;
import com.trungtam.report.dto.response.StudentClassOption;
import com.trungtam.report.dto.response.StudentClassSummaryResponse;
import com.trungtam.report.dto.response.TopicMasteryItem;
import com.trungtam.report.service.StudentReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Bao cao cap HOC VIEN. Scope: canViewStudentReport (STUDENT chinh minh,
 * PARENT con, TEACHER/ASSISTANT chung lop, ADMIN/EMPLOYEE tat ca).
 */
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class StudentReportController {

    private static final String CAN_VIEW =
            "hasAuthority('REPORT:READ') and @securityService.canViewStudentReport(#studentId, authentication)";

    private final StudentReportService studentReportService;

    @GetMapping("/students/{studentId}/recent-exams")
    @PreAuthorize(CAN_VIEW)
    public ApiResponse<List<RecentExamItem>> recentExams(
            @PathVariable Long studentId,
            @RequestParam(defaultValue = "3") int limit) {
        return ApiResponse.ok(studentReportService.recentExams(studentId, limit));
    }

    @GetMapping("/students/{studentId}/exam-history")
    @PreAuthorize(CAN_VIEW)
    public ApiResponse<List<RecentExamItem>> examHistory(
            @PathVariable Long studentId,
            @RequestParam Long classId) {
        return ApiResponse.ok(studentReportService.examHistory(studentId, classId));
    }

    @GetMapping("/students/{studentId}/exams/{examId}")
    @PreAuthorize(CAN_VIEW)
    public ApiResponse<ExamReportDetail> examDetail(
            @PathVariable Long studentId,
            @PathVariable Long examId,
            @RequestParam Long classId) {
        return ApiResponse.ok(studentReportService.examDetail(studentId, examId, classId));
    }

    @GetMapping("/students/{studentId}/classes/{classId}/score-trend")
    @PreAuthorize(CAN_VIEW)
    public ApiResponse<List<ScoreTrendPoint>> scoreTrend(
            @PathVariable Long studentId,
            @PathVariable Long classId) {
        return ApiResponse.ok(studentReportService.scoreTrend(studentId, classId));
    }

    @GetMapping("/students/{studentId}/classes/{classId}/breakdowns")
    @PreAuthorize(CAN_VIEW)
    public ApiResponse<BreakdownResponse> breakdowns(
            @PathVariable Long studentId,
            @PathVariable Long classId,
            @RequestParam(required = false) Long examId) {
        return ApiResponse.ok(studentReportService.breakdowns(studentId, classId, examId));
    }

    @GetMapping("/students/{studentId}/classes/{classId}/topic-mastery")
    @PreAuthorize(CAN_VIEW)
    public ApiResponse<List<TopicMasteryItem>> topicMastery(
            @PathVariable Long studentId,
            @PathVariable Long classId) {
        return ApiResponse.ok(studentReportService.topicMastery(studentId, classId));
    }

    @GetMapping("/students/{studentId}/classes/{classId}/attendance")
    @PreAuthorize(CAN_VIEW)
    public ApiResponse<StudentAttendanceReport> attendance(
            @PathVariable Long studentId,
            @PathVariable Long classId) {
        return ApiResponse.ok(studentReportService.attendance(studentId, classId));
    }

    @GetMapping("/students/{studentId}/classes/{classId}/summary")
    @PreAuthorize(CAN_VIEW)
    public ApiResponse<StudentClassSummaryResponse> summary(
            @PathVariable Long studentId,
            @PathVariable Long classId) {
        return ApiResponse.ok(studentReportService.summary(studentId, classId));
    }

    @GetMapping("/students/{studentId}/classes")
    @PreAuthorize(CAN_VIEW)
    public ApiResponse<List<StudentClassOption>> listClasses(@PathVariable Long studentId) {
        return ApiResponse.ok(studentReportService.listClasses(studentId));
    }

    @GetMapping("/my-children")
    @PreAuthorize("hasAuthority('REPORT:READ')")
    public ApiResponse<List<ChildOption>> myChildren() {
        return ApiResponse.ok(studentReportService.myChildren());
    }

    @GetMapping("/my-classes")
    @PreAuthorize("hasAuthority('REPORT:READ')")
    public ApiResponse<List<StudentClassOption>> myClasses() {
        return ApiResponse.ok(studentReportService.myClasses());
    }
}
