package com.trungtam.exam.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.trungtam.exam.entity.Exam;
import com.trungtam.schoolclass.entity.SchoolClass;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExamDetailResponse(
        Long id,
        String code,
        String name,
        Long subjectId,
        String subjectName,
        String gradeLevel,
        Long topicId,
        String topicName,
        String type,
        Integer durationMinutes,
        Instant publishAt,
        Instant endAt,
        String status,
        List<ExamExerciseResponse> exercises,
        List<ClassRefResponse> classes,
        List<StudentOptionResponse> students,
        String createdBy,
        String updatedBy,
        Instant createdAt,
        Instant updatedAt
) {
    public record ClassRefResponse(Long id, String code, String name) {
        public static ClassRefResponse from(SchoolClass c) {
            return new ClassRefResponse(c.getId(), c.getCode(), c.getName());
        }
    }

    public static ExamDetailResponse from(Exam e) {
        return new ExamDetailResponse(
                e.getId(),
                e.getCode(),
                e.getName(),
                e.getSubject().getId(),
                e.getSubject().getName(),
                e.getSubject().getGradeLevel(),
                e.getTopic() != null ? e.getTopic().getId() : null,
                e.getTopic() != null ? e.getTopic().getName() : null,
                e.getType().name(),
                e.getDurationMinutes(),
                e.getPublishAt(),
                e.getEndAt(),
                e.getStatus().name(),
                e.getExamExercises().stream().map(ExamExerciseResponse::from).toList(),
                e.getClasses().stream().map(ClassRefResponse::from).toList(),
                e.getStudents().stream().map(StudentOptionResponse::from).toList(),
                e.getCreatedBy(),
                e.getUpdatedBy(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
