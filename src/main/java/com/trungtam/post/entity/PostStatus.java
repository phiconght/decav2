package com.trungtam.post.entity;

/**
 * Vong doi bai viet: DRAFT (nhap, chi admin thay) -> PUBLISHED (mobile thay)
 * -> ARCHIVED (go khoi Home, khong xoa).
 */
public enum PostStatus {
    DRAFT,
    PUBLISHED,
    ARCHIVED
}
