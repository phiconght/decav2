package com.trungtam.report.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Chi tiet bao cao 1 bai thi cua 1 HV trong 1 lop.
 * Tang DIEM (theo bai): score/classAverage/rank/maxScore + pho diem (distribution).
 * Tang NANG LUC (theo CHUONG cua de): topicId/topicName + breakdown (tong hop moi bai
 * thuoc chuong do; de khong gan chuong -> breakdown toan khoa, topicId=null).
 */
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
        Long topicId,
        String topicName,
        BreakdownResponse breakdown,
        ExamScoreDistribution distribution
) {
}
