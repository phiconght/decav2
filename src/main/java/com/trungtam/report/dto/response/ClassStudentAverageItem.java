package com.trungtam.report.dto.response;

/** 1 dong trong bang HV cua lop: diem TB + chuyen can. */
public record ClassStudentAverageItem(
        Long studentId,
        String fullName,
        String username,
        long submittedCount,
        java.math.BigDecimal avgScore,
        Double avgPct,
        Double attendanceRate
) {
}
