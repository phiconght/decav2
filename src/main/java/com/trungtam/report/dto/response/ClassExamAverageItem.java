package com.trungtam.report.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

/** TB lop cua 1 de (cho bieu do TB lop qua cac de). */
public record ClassExamAverageItem(
        Long examId,
        String examName,
        Instant publishAt,
        BigDecimal avgScore,
        BigDecimal maxScore,
        long submittedCount,
        long assignedCount
) {
}
