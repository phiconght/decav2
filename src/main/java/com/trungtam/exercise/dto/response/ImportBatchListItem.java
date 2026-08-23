package com.trungtam.exercise.dto.response;

import com.trungtam.exercise.entity.ImportBatch;

import java.time.Instant;

/** 1 dong trong danh sach lo nhap (badge dem + man "danh sach lo neu > 1"). */
public record ImportBatchListItem(
        Long id,
        String sourceFileName,
        String subjectName,
        String gradeLevel,
        String examName,
        int totalCount,
        String status,
        Instant createdAt
) {
    public static ImportBatchListItem from(ImportBatch b) {
        return new ImportBatchListItem(
                b.getId(),
                b.getSourceFileName(),
                b.getSubject().getName(),
                b.getSubject().getGradeLevel(),
                b.getExamName(),
                b.getTotalCount(),
                b.getStatus().name(),
                b.getCreatedAt()
        );
    }
}
