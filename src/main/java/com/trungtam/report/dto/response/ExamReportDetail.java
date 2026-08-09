package com.trungtam.report.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Chi tiet bao cao 1 bai thi cua 1 HV trong 1 lop.
 * Tang DIEM (theo bai): score/classAverage/rank/maxScore + pho diem (distribution).
 * Tang NANG LUC (theo CHUONG cua de): topicId/topicName + breakdown (tong hop moi bai
 * thuoc chuong do; de khong gan chuong -> breakdown toan khoa, topicId=null).
 * topicId/topicName/sessionId/sessionTitle/sessionDate: NGU CANH — de biet bai thi
 * nay thuoc chuong nao/buoi nao (§Phan C), null neu de chua gan chuong/buoi.
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
        Long sessionId,
        String sessionTitle,
        LocalDate sessionDate,
        BreakdownResponse breakdown,
        ExamScoreDistribution distribution
) {
}
