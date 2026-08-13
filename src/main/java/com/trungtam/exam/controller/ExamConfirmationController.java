package com.trungtam.exam.controller;

import com.trungtam.common.dto.ApiResponse;
import com.trungtam.exam.dto.request.ConfirmExamsRequest;
import com.trungtam.exam.dto.request.ExamConfirmationSearchParams;
import com.trungtam.exam.dto.response.ExamConfirmationItem;
import com.trungtam.exam.dto.response.ExamConfirmationPageResponse;
import com.trungtam.exam.service.ExamConfirmationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Xac nhan bai thi da nop truoc khi tinh vao bao cao (yeu cau nguoi dung
 * 13/08/2026). Danh cho nhan vien/GV/Admin (quyen EXAM:CONFIRM).
 */
@RestController
@RequestMapping("/api/v1/exam-confirmations")
@RequiredArgsConstructor
public class ExamConfirmationController {

    private final ExamConfirmationService examConfirmationService;

    @GetMapping
    @PreAuthorize("hasAuthority('EXAM:CONFIRM')")
    public ExamConfirmationPageResponse listPending(@ModelAttribute ExamConfirmationSearchParams params) {
        return examConfirmationService.listPending(params);
    }

    @PatchMapping("/{examStudentId}/confirm")
    @PreAuthorize("hasAuthority('EXAM:CONFIRM')")
    public ApiResponse<ExamConfirmationItem> confirm(@PathVariable Long examStudentId) {
        return ApiResponse.ok(examConfirmationService.confirm(examStudentId));
    }

    @PatchMapping("/confirm-bulk")
    @PreAuthorize("hasAuthority('EXAM:CONFIRM')")
    public ApiResponse<Integer> confirmBulk(@Valid @RequestBody ConfirmExamsRequest request) {
        return ApiResponse.ok(examConfirmationService.confirmBulk(request.examStudentIds()));
    }
}
