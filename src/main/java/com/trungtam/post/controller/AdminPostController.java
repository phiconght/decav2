package com.trungtam.post.controller;

import com.trungtam.common.dto.ApiResponse;
import com.trungtam.post.dto.request.CreatePostRequest;
import com.trungtam.post.dto.request.PostSearchParams;
import com.trungtam.post.dto.request.UpdatePinRequest;
import com.trungtam.post.dto.request.UpdatePostStatusRequest;
import com.trungtam.post.dto.response.PostDetail;
import com.trungtam.post.dto.response.PostPageResponse;
import com.trungtam.post.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * Quan tri bai viet (ADMIN / EMPLOYEE) — moi trang thai.
 */
@RestController
@RequestMapping("/api/v1/admin/posts")
@RequiredArgsConstructor
public class AdminPostController {

    private final PostService postService;

    @GetMapping
    @PreAuthorize("hasAuthority('POST:READ')")
    public PostPageResponse list(@ModelAttribute PostSearchParams params) {
        return postService.adminList(params);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('POST:READ')")
    public ApiResponse<PostDetail> detail(@PathVariable Long id) {
        return ApiResponse.ok(postService.adminDetail(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('POST:WRITE')")
    public ApiResponse<PostDetail> create(@Valid @RequestBody CreatePostRequest req) {
        return ApiResponse.ok(postService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('POST:WRITE')")
    public ApiResponse<PostDetail> update(
            @PathVariable Long id, @Valid @RequestBody CreatePostRequest req) {
        return ApiResponse.ok(postService.update(id, req));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('POST:WRITE')")
    public ApiResponse<PostDetail> changeStatus(
            @PathVariable Long id, @Valid @RequestBody UpdatePostStatusRequest req) {
        return ApiResponse.ok(postService.changeStatus(id, req.status()));
    }

    @PatchMapping("/{id}/pin")
    @PreAuthorize("hasAuthority('POST:WRITE')")
    public ApiResponse<PostDetail> pin(
            @PathVariable Long id, @RequestBody UpdatePinRequest req) {
        return ApiResponse.ok(postService.setPinned(id, req.pinned()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('POST:WRITE')")
    public ApiResponse<Void> archive(@PathVariable Long id) {
        postService.archive(id);
        return ApiResponse.ok();
    }
}
