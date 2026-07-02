package com.trungtam.post.dto.request;

import com.trungtam.post.entity.PostStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Doi trang thai bai viet (dang / go / luu tru).
 */
public record UpdatePostStatusRequest(@NotNull PostStatus status) {
}
