package com.trungtam.report.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

/** 1 bai da nop (cho danh sach gan nhat / lich su). */
public record RecentExamItem(
        Long examStudentId,
        Long examId,
        String examCode,
        String examName,
        String subjectName,
        Long classId,
        String className,
        Instant submittedAt,
        BigDecimal score,
        BigDecimal maxScore
) {
}
