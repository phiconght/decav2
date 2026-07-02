package com.trungtam.report.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

/** 1 diem tren bieu do xu huong diem (diem cua HV + TB lop cua de do). */
public record ScoreTrendPoint(
        Long examId,
        String examName,
        Instant publishAt,
        Instant submittedAt,
        BigDecimal score,
        BigDecimal maxScore,
        BigDecimal classAverage
) {
}
