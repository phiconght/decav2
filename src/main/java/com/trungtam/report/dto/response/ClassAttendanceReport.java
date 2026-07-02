package com.trungtam.report.dto.response;

import java.util.List;

/** Bao cao chuyen can ca lop: tong hop + theo thang. */
public record ClassAttendanceReport(
        AttendanceSummary summary,
        List<AttendanceMonthPoint> byMonth
) {
}
