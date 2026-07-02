package com.trungtam.report.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Pho diem cua lop cho 1 bai thi (hoac diem TB khoa — §12.2).
 * studentScore/studentBandIndex/percentile = null khi goi o cap lop (khong gan HV).
 * percentile = % ban co diem <= diem HV (0..100).
 */
public record ExamScoreDistribution(
        Long examId,
        String examName,
        BigDecimal maxScore,
        int bandCount,
        List<ScoreBand> bands,
        BigDecimal studentScore,
        Integer studentBandIndex,
        Double percentile,
        BigDecimal classAverage,
        BigDecimal median,
        BigDecimal highest,
        BigDecimal lowest,
        Integer rank,
        Integer submittedCount,
        Integer classSize
) {
}
