package com.trungtam.exercise.controller;

import com.trungtam.common.dto.ApiResponse;
import com.trungtam.exercise.dto.request.ConfirmExercisesRequest;
import com.trungtam.exercise.dto.request.CreateExerciseRequest;
import com.trungtam.exercise.dto.request.ExerciseSearchParams;
import com.trungtam.exercise.dto.request.UpdateStatusRequest;
import com.trungtam.exercise.dto.response.ExerciseDetailResponse;
import com.trungtam.exercise.dto.response.ExercisePageResponse;
import com.trungtam.exercise.service.ExerciseService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/exercises")
@RequiredArgsConstructor
public class ExerciseController {

    private final ExerciseService exerciseService;

    /** Tim kiem + phan trang bai tap. */
    @GetMapping
    @PreAuthorize("hasAuthority('EXERCISE:READ')")
    public ExercisePageResponse search(@ModelAttribute ExerciseSearchParams params) {
        return exerciseService.search(params);
    }

    /** Lay chi tiet 1 bai tap (day du dap an theo loai). */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('EXERCISE:READ')")
    public ApiResponse<ExerciseDetailResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(exerciseService.getById(id));
    }

    /** Tao bai tap moi. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('EXERCISE:WRITE')")
    public ApiResponse<ExerciseDetailResponse> create(@Valid @RequestBody CreateExerciseRequest request) {
        return ApiResponse.ok(exerciseService.create(request));
    }

    /** Cap nhat toan bo bai tap (xoa dap an cu, ghi dap an moi). */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EXERCISE:WRITE')")
    public ApiResponse<ExerciseDetailResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateExerciseRequest request) {
        return ApiResponse.ok(exerciseService.update(id, request));
    }

    /** Doi trang thai nhanh (ACTIVE <-> INACTIVE). */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('EXERCISE:WRITE')")
    public ApiResponse<ExerciseDetailResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request) {
        exerciseService.updateStatus(id, request);
        return ApiResponse.ok(exerciseService.getById(id));
    }

    /** Xoa bai tap + toan bo dap an (CASCADE). */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('EXERCISE:DELETE')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        exerciseService.delete(id);
        return ApiResponse.ok();
    }

    // ---- Nhap theo lo (import batch) — xem SPEC_NhapBaiTap_TuWord_QuaAI.md §6.1 ----

    /** PENDING -> ACTIVE cho 1 bai (nut "Xac nhan" tren man duyet lo). */
    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('EXERCISE:WRITE')")
    public ApiResponse<ExerciseDetailResponse> confirm(@PathVariable Long id) {
        return ApiResponse.ok(exerciseService.confirm(id));
    }

    /** Xac nhan hang loat (nut "Xac nhan da chon"). */
    @PostMapping("/confirm-batch")
    @PreAuthorize("hasAuthority('EXERCISE:WRITE')")
    public ApiResponse<Void> confirmBatch(@Valid @RequestBody ConfirmExercisesRequest request) {
        exerciseService.confirmBatch(request.ids());
        return ApiResponse.ok();
    }

    /** DELETED -> PENDING, chi trong luc lo van dang duyet (IN_PROGRESS). */
    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAuthority('EXERCISE:WRITE')")
    public ApiResponse<ExerciseDetailResponse> restore(@PathVariable Long id) {
        return ApiResponse.ok(exerciseService.restore(id));
    }
}
