package com.trungtam.post.dto.request;

import com.trungtam.post.entity.PostStatus;
import lombok.Getter;
import lombok.Setter;

/**
 * Tham so loc / phan trang danh sach bai viet (quan tri).
 */
@Getter
@Setter
public class PostSearchParams {
    private String title;
    private PostStatus status;
    private int current = 1;
    private int pageSize = 10;
}
