package com.trungtam.exam.dto.response;

import com.trungtam.exam.entity.Exam;

import java.time.Instant;

public record ExamListItem(
        Long id,
        String code,
        String name,
        Long subjectId,
        String subjectName,
        String gradeLevel,
        String type,
        Integer durationMinutes,
        Instant publishAt,
        Instant endAt,
        String status,
        int exerciseCount,
        int classCount,
        int studentCount,
        String createdBy,
        Instant createdAt
) {
    public static ExamListItem from(Exam e) {
        return new ExamListItem(
                e.getId(),
                e.getCode(),
                e.getName(),
                e.getSubject().getId(),
                e.getSubject().getName(),
                e.getSubject().getGradeLevel(),
                e.getType().name(),
                e.getDurationMinutes(),
                e.getPublishAt(),
                e.getEndAt(),
                e.getStatus().name(),
                e.getExamExercises().size(),
                e.getClasses().size(),
                e.getStudents().size(),
                e.getCreatedBy(),
                e.getCreatedAt()
        );
    }
}
