package com.trungtam.post.dto.response;

import com.trungtam.post.entity.Post;
import com.trungtam.post.entity.PostStatus;

import java.time.Instant;

/**
 * Chi tiet bai viet (kem content_md day du).
 */
public record PostDetail(
        Long id,
        String title,
        String summary,
        String coverImageUrl,
        String contentMd,
        PostStatus status,
        boolean pinned,
        Instant publishedAt,
        Instant createdAt,
        String author
) {
    public static PostDetail from(Post p) {
        return new PostDetail(
                p.getId(),
                p.getTitle(),
                p.getSummary(),
                p.getCoverImageUrl(),
                p.getContentMd(),
                p.getStatus(),
                p.isPinned(),
                p.getPublishedAt(),
                p.getCreatedAt(),
                p.getCreatedBy());
    }
}
