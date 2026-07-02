package com.trungtam.report.dto.response;

import java.util.List;

/** Bao cao chuyen can cua 1 HV trong 1 lop: tong hop + theo thang. */
public record StudentAttendanceReport(
        AttendanceSummary summary,
        List<AttendanceMonthPoint> byMonth
) {
}
