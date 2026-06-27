package com.trungtam.exam.controller;

import com.trungtam.common.dto.ApiResponse;
import com.trungtam.exam.dto.request.CreateExamRequest;
import com.trungtam.exam.dto.request.ExamSearchParams;
import com.trungtam.exam.dto.request.UpdateExamStatusRequest;
import com.trungtam.exam.dto.request.UpdateExamStudentStatusRequest;
import com.trungtam.exam.dto.response.ExamClassItem;
import com.trungtam.exam.dto.response.ExamDetailResponse;
import com.trungtam.exam.dto.response.ExamListItem;
import com.trungtam.exam.dto.response.ExamPageResponse;
import com.trungtam.exam.dto.response.StudentExamItem;
import com.trungtam.exam.dto.response.StudentOptionResponse;
import com.trungtam.exam.service.ExamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/exams")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    @GetMapping
    @PreAuthorize("hasAuthority('EXAM:READ')")
    public ExamPageResponse search(@ModelAttribute ExamSearchParams params) {
        return examService.search(params);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('EXAM:READ')")
    public ApiResponse<ExamDetailResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(examService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('EXAM:WRITE')")
    public ApiResponse<ExamDetailResponse> create(@Valid @RequestBody CreateExamRequest request) {
        return ApiResponse.ok(examService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EXAM:WRITE')")
    public ApiResponse<ExamDetailResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateExamRequest request) {
        return ApiResponse.ok(examService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('EXAM:WRITE')")
    public ApiResponse<ExamDetailResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateExamStatusRequest request) {
        return ApiResponse.ok(examService.updateStatus(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('EXAM:DELETE')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        examService.delete(id);
        return ApiResponse.ok();
    }

    @GetMapping("/student-options")
    @PreAuthorize("hasAuthority('EXAM:READ')")
    public ApiResponse<List<StudentOptionResponse>> studentOptions(
            @RequestParam(required = false) List<Long> classIds) {
        return ApiResponse.ok(examService.listStudentOptions(classIds));
    }

    /** Danh sach de thi cua 1 lop (self-scoped cho hoc vien da dang nhap). */
    @GetMapping("/by-class/{classId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<ExamListItem>> listByClass(@PathVariable Long classId) {
        return ApiResponse.ok(examService.listExamsByClass(classId));
    }

    /** Danh sach khoa hoc cua 1 de (popup cot "So khoa"). */
    @GetMapping("/{examId}/classes")
    @PreAuthorize("hasAuthority('EXAM:READ')")
    public ApiResponse<List<ExamClassItem>> listExamClasses(@PathVariable Long examId) {
        return ApiResponse.ok(examService.listExamClasses(examId));
    }

    /** Danh sach de thi cua 1 hoc vien trong 1 khoa (popup man Hoc vien). */
    @GetMapping("/student/{userId}/class/{classId}")
    @PreAuthorize("hasAuthority('EXAM:READ')")
    public ApiResponse<List<StudentExamItem>> listStudentExams(
            @PathVariable Long userId,
            @PathVariable Long classId) {
        return ApiResponse.ok(examService.listStudentExamsInClass(userId, classId));
    }

    /** Admin doi trang thai de cho 1 hoc vien. */
    @PatchMapping("/{examId}/students/{userId}/status")
    @PreAuthorize("hasAuthority('EXAM:WRITE')")
    public ApiResponse<Void> updateStudentStatus(
            @PathVariable Long examId,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateExamStudentStatusRequest request) {
        examService.updateExamStudentStatus(examId, userId, request);
        return ApiResponse.ok();
    }
}
