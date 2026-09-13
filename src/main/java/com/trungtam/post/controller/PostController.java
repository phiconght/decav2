package com.trungtam.post.controller;

import com.trungtam.common.dto.ApiResponse;
import com.trungtam.post.dto.response.PostDetail;
import com.trungtam.post.dto.response.PostPageResponse;
import com.trungtam.post.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Feed bai viet cho Mobile + Trang chu cong khai Web — chi bai da PUBLISHED.
 * {@code permitAll()} — khach chua dang nhap cung xem duoc
 * (KEHOACH_WEB_TrangChuCongKhai_HeroContent.md); PostService da loc PUBLISHED
 * nen khong lo bai DRAFT/ARCHIVED.
 */
@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping
    @PreAuthorize("permitAll()")
    public PostPageResponse feed(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "5") int pageSize) {
        return postService.feed(current, pageSize);
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public ApiResponse<PostDetail> detail(@PathVariable Long id) {
        return ApiResponse.ok(postService.publicDetail(id));
    }
}
