package com.trungtam.report.controller;

import com.trungtam.common.dto.ApiResponse;
import com.trungtam.report.dto.request.CreateCommentRequest;
import com.trungtam.report.dto.request.UpdateCommentRequest;
import com.trungtam.report.dto.response.CommentItem;
import com.trungtam.report.service.ReportCommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Nhan xet bao cao. Scope doc theo canViewStudentReport; them/sua/xoa can REPORT:COMMENT.
 */
@RestController
@RequestMapping("/api/v1/reports/comments")
@RequiredArgsConstructor
public class ReportCommentController {

    private final ReportCommentService reportCommentService;

    @GetMapping
    @PreAuthorize("hasAuthority('REPORT:READ') and @securityService.canViewStudentReport(#studentId, authentication)")
    public ApiResponse<List<CommentItem>> list(
            @RequestParam Long studentId,
            @RequestParam Long classId,
            @RequestParam(required = false) Long examStudentId) {
        return ApiResponse.ok(reportCommentService.list(studentId, classId, examStudentId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('REPORT:COMMENT') "
            + "and @securityService.canViewStudentReport(#request.studentId(), authentication)")
    public ApiResponse<CommentItem> create(@Valid @RequestBody CreateCommentRequest request) {
        return ApiResponse.ok(reportCommentService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('REPORT:COMMENT')")
    public ApiResponse<CommentItem> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCommentRequest request) {
        return ApiResponse.ok(reportCommentService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('REPORT:COMMENT') or hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        reportCommentService.delete(id);
        return ApiResponse.ok();
    }
}
