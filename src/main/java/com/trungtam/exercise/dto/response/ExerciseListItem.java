package com.trungtam.exercise.dto.response;

import com.trungtam.exercise.entity.Exercise;

import java.time.Instant;

public record ExerciseListItem(
        Long id,
        String code,
        String title,
        Long subjectId,
        String subjectName,
        String gradeLevel,
        Long topicId,
        String topicName,
        String type,
        String difficulty,
        String status,
        String createdBy,
        Instant createdAt
) {
    public static ExerciseListItem from(Exercise e) {
        return new ExerciseListItem(
                e.getId(),
                e.getCode(),
                e.getTitle(),
                e.getSubjectEntity().getId(),
                e.getSubjectEntity().getName(),
                e.getSubjectEntity().getGradeLevel(),
                e.getTopic() != null ? e.getTopic().getId() : null,
                e.getTopic() != null ? e.getTopic().getName() : null,
                e.getType().name(),
                e.getDifficulty().name(),
                e.getStatus().name(),
                e.getCreatedBy(),
                e.getCreatedAt()
        );
    }
}
