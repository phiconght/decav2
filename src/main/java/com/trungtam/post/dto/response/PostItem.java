package com.trungtam.post.dto.response;

import com.trungtam.post.entity.Post;
import com.trungtam.post.entity.PostStatus;

import java.time.Instant;

/**
 * Dong danh sach bai viet (KHONG kem content_md — nhe cho feed / bang quan tri).
 */
public record PostItem(
        Long id,
        String title,
        String summary,
        String coverImageUrl,
        PostStatus status,
        boolean pinned,
        Instant publishedAt,
        Instant createdAt,
        String author
) {
    public static PostItem from(Post p) {
        return new PostItem(
                p.getId(),
                p.getTitle(),
                p.getSummary(),
                p.getCoverImageUrl(),
                p.getStatus(),
                p.isPinned(),
                p.getPublishedAt(),
                p.getCreatedAt(),
                p.getCreatedBy());
    }
}
