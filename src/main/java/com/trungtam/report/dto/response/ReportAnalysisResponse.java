package com.trungtam.report.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Bang "Phan tich tu dong" o dau bao cao ca nhan cua 1 HV: cau chu tieng
 * Viet da sinh san o BE (khong lap logic nguong o ADMIN/Mobile).
 */
public record ReportAnalysisResponse(
        String studentName,
        String className,
        String scoreSpectrumLabel,
        BigDecimal courseAverage,
        Integer courseRank,
        Integer classSize,
        List<ChapterAnalysisItem> chapters,
        List<String> abilityInsights,
        String attendanceInsight,
        String teacherCommentAuthor,
        String teacherCommentContent
) {
    public record ChapterAnalysisItem(
            Long topicId,
            String chapterLabel,
            BigDecimal avgScore,
            Integer rank,
            Integer classSize
    ) {
    }
}
