package com.trungtam.common.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Bao phan trang gon nhe, tach client khoi cau truc Page noi bo cua Spring.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
