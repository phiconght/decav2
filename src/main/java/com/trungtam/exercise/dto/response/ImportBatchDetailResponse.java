package com.trungtam.exercise.dto.response;

import com.trungtam.exercise.entity.ImportBatch;

import java.time.Instant;
import java.util.List;

/** Chi tiet 1 lo nhap + toan bo bai tap trong lo (du de render man duyet lo). */
public record ImportBatchDetailResponse(
        Long id,
        String sourceFileName,
        Long subjectId,
        String subjectName,
        String gradeLevel,
        Long topicId,
        String topicName,
        String examName,
        Long examId,
        int totalCount,
        int confirmedCount,
        String status,
        Instant createdAt,
        List<ExerciseDetailResponse> exercises
) {
    public static ImportBatchDetailResponse from(ImportBatch b, int confirmedCount,
                                                  List<ExerciseDetailResponse> exercises) {
        return new ImportBatchDetailResponse(
                b.getId(),
                b.getSourceFileName(),
                b.getSubject().getId(),
                b.getSubject().getName(),
                b.getSubject().getGradeLevel(),
                b.getTopic() != null ? b.getTopic().getId() : null,
                b.getTopic() != null ? b.getTopic().getName() : null,
                b.getExamName(),
                b.getExam() != null ? b.getExam().getId() : null,
                b.getTotalCount(),
                confirmedCount,
                b.getStatus().name(),
                b.getCreatedAt(),
                exercises
        );
    }
}
