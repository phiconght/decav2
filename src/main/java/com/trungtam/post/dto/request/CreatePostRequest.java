package com.trungtam.post.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Yeu cau tao / sua bai viet.
 */
public record CreatePostRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 500) String summary,
        @Size(max = 500) String coverImageUrl,
        @NotBlank String contentMd,
        boolean pinned
) {
}
