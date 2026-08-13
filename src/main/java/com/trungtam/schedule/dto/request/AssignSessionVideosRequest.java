package com.trungtam.schedule.dto.request;

import java.util.List;

/**
 * Gan (ghi de) toan bo danh sach video cho 1 buoi hoc. Thu tu trong
 * {@code videoIds} la thu tu hien thi. Danh sach rong = go het video khoi buoi.
 */
public record AssignSessionVideosRequest(
        List<Long> videoIds
) {}
