package com.trungtam.report.dto.response;

/** Chuyen can theo thang (yyyy-MM) cho bieu do cot chong. */
public record AttendanceMonthPoint(
        String month,
        long coMat,
        long tre,
        long vang,
        long coPhep
) {
}
