package com.trungtam.exam.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * De thi gan RIENG 1 buoi hoc, kem trang thai bai lam cua nguoi dang xem (neu
 * la hoc vien). Dung cho SessionDetailPage (Mobile) — xem 1 buoi la thay
 * ngay de thi cua buoi do, bam vao lam bai luon.
 */
public record SessionExamItem(
        Long examId,
        String code,
        String name,
        String type,
        String status,
        Instant publishAt,
        Instant endAt,
        Integer durationMinutes,
        /** Trang thai bai lam cua nguoi dang xem (null neu khong phai hoc vien / chua co dong). */
        String studentStatus,
        BigDecimal score
) {}
