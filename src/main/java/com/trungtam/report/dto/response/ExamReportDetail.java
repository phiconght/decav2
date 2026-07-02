package com.trungtam.report.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

/** Chi tiet bao cao 1 bai thi cua 1 HV trong 1 lop. */
public record ExamReportDetail(
        Long examId,
        String examName,
        String examCode,
        Instant submittedAt,
        BigDecimal score,
        BigDecimal maxScore,
        BigDecimal classAverage,
        Integer rank,
        Integer submittedCount,
        Integer classSize,
        BreakdownResponse breakdown
) {
}
