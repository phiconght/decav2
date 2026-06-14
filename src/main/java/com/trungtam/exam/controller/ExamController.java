package com.trungtam.exam.controller;

import com.trungtam.common.dto.ApiResponse;
import com.trungtam.exam.dto.request.CreateExamRequest;
import com.trungtam.exam.dto.request.ExamSearchParams;
import com.trungtam.exam.dto.request.UpdateExamStatusRequest;
import com.trungtam.exam.dto.response.ExamDetailResponse;
import com.trungtam.exam.dto.response.ExamPageResponse;
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
}
