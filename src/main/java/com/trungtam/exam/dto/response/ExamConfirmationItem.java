package com.trungtam.exam.dto.response;

import com.trungtam.exam.entity.ExamStudent;

import java.math.BigDecimal;
import java.time.Instant;

/** 1 dong bai thi da nop, cho xac nhan de tinh vao bao cao. */
public record ExamConfirmationItem(
        Long examStudentId,
        Long examId,
        String examCode,
        String examName,
        Long studentId,
        String studentName,
        BigDecimal score,
        Instant submittedAt
) {
    public static ExamConfirmationItem from(ExamStudent es) {
        return new ExamConfirmationItem(
                es.getId(),
                es.getExam().getId(),
                es.getExam().getCode(),
                es.getExam().getName(),
                es.getUser().getId(),
                es.getUser().getFullName(),
                es.getScore(),
                es.getSubmittedAt());
    }
}
