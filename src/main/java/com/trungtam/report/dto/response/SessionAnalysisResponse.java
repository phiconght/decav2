package com.trungtam.report.dto.response;

import java.math.BigDecimal;
import java.util.List;

/** Bang "Phan tich tu dong" o dau bao cao BUOI HOC — Phan C. */
public record SessionAnalysisResponse(
        BigDecimal avgScore,
        BigDecimal classAverage,
        String comparisonInsight,
        List<String> abilityInsights,
        int examCount,
        int submittedCount
) {
}
