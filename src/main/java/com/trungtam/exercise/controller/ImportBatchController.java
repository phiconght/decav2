package com.trungtam.exercise.controller;

import com.trungtam.common.dto.ApiResponse;
import com.trungtam.exercise.dto.response.ImportBatchDetailResponse;
import com.trungtam.exercise.dto.response.ImportBatchListItem;
import com.trungtam.exercise.service.ExerciseImportBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Nhap bai tap/de thi theo lo tu file du lieu (.json, Word -> AI boc tach) —
 * xem SPEC_NhapBaiTap_TuWord_QuaAI.md §6.1.
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class ImportBatchController {

    private final ExerciseImportBatchService importBatchService;

    @PostMapping(value = "/exercises/import-batch", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('EXERCISE:WRITE')")
    public ApiResponse<ImportBatchDetailResponse> importBatch(
            @RequestParam("data") MultipartFile data,
            @RequestParam Long subjectId,
            @RequestParam(required = false) Long topicId,
            @RequestParam(required = false) String examName) {
        return ApiResponse.ok(importBatchService.importBatch(data, subjectId, topicId, examName));
    }

    @GetMapping("/import-batches")
    @PreAuthorize("hasAuthority('EXERCISE:READ')")
    public ApiResponse<List<ImportBatchListItem>> myBatches() {
        return ApiResponse.ok(importBatchService.myBatches());
    }

    @GetMapping("/import-batches/in-progress-count")
    @PreAuthorize("hasAuthority('EXERCISE:READ')")
    public ApiResponse<Long> inProgressCount() {
        return ApiResponse.ok(importBatchService.myInProgressCount());
    }

    @GetMapping("/import-batches/{id}")
    @PreAuthorize("hasAuthority('EXERCISE:READ')")
    public ApiResponse<ImportBatchDetailResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(importBatchService.getById(id));
    }
}
