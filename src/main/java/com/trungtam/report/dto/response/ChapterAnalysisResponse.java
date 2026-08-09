package com.trungtam.report.dto.response;

import java.math.BigDecimal;
import java.util.List;

/** Bang "Phan tich tu dong" o dau bao cao CHUONG — Phan C. */
public record ChapterAnalysisResponse(
        String chapterLabel,
        BigDecimal avgScore,
        Integer rank,
        Integer classSize,
        List<String> abilityInsights,
        String attendanceInsight
) {
}
