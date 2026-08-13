package com.trungtam.video.controller;

import com.trungtam.common.dto.ApiResponse;
import com.trungtam.video.dto.request.LectureVideoRequest;
import com.trungtam.video.dto.request.LectureVideoSearchParams;
import com.trungtam.video.dto.response.LectureVideoItem;
import com.trungtam.video.dto.response.LectureVideoPageResponse;
import com.trungtam.video.service.LectureVideoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Kho video bai giang — CRUD + tim de tai su dung.
 * Khong guard theo lop (video khong thuoc rieng 1 lop) — xem SPEC_VideoBaiGiang_Zoom.md §3.1a.
 */
@RestController
@RequestMapping("/api/v1/lecture-videos")
@RequiredArgsConstructor
public class LectureVideoController {

    private final LectureVideoService videoService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public LectureVideoPageResponse search(@ModelAttribute LectureVideoSearchParams params) {
        return videoService.search(params);
    }

    /** Danh sach ngan gon khong phan trang — dropdown/search-and-add khi gan video vao buoi. */
    @GetMapping("/quick-search")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<LectureVideoItem>> quickSearch(@ModelAttribute LectureVideoSearchParams params) {
        return ApiResponse.ok(videoService.quickSearch(params));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<LectureVideoItem> getById(@PathVariable Long id) {
        return ApiResponse.ok(videoService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SESSION_CONTENT:WRITE')")
    public ApiResponse<LectureVideoItem> create(@Valid @RequestBody LectureVideoRequest request) {
        return ApiResponse.ok(videoService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SESSION_CONTENT:WRITE')")
    public ApiResponse<LectureVideoItem> update(
            @PathVariable Long id,
            @Valid @RequestBody LectureVideoRequest request) {
        return ApiResponse.ok(videoService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SESSION_CONTENT:WRITE')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        videoService.delete(id);
        return ApiResponse.ok();
    }
}
