package com.trungtam.report.dto.response;

/**
 * Tong hop chuyen can (chi tinh buoi DONE co diem danh).
 * attendanceRate = (coMat+tre)/totalSessions; onTimeRate = coMat/(coMat+tre).
 * vang = VANG + CHUA_CHECKIN (buoi da dien ra ma chua check-in).
 */
public record AttendanceSummary(
        long totalSessions,
        long coMat,
        long tre,
        long vang,
        long coPhep,
        long chuaCheckin,
        Double attendanceRate,
        Double onTimeRate
) {
}
