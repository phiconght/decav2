package com.trungtam.report.dto.response;

import java.math.BigDecimal;
import java.util.List;

/** Bang "Phan tich tu dong" o dau bao cao BAI THI — Phan C. */
public record ExamAnalysisResponse(
        BigDecimal score,
        BigDecimal classAverage,
        Integer rank,
        Integer classSize,
        String comparisonInsight,
        List<String> abilityInsights
) {
}
