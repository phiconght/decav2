package com.trungtam.exercise.dto.response;

import com.trungtam.exercise.entity.Exercise;

import java.time.Instant;

public record ExerciseListItem(
        Long id,
        String code,
        String title,
        String gradeLevel,
        String subject,
        String type,
        String status,
        String createdBy,
        Instant createdAt
) {
    public static ExerciseListItem from(Exercise e) {
        return new ExerciseListItem(
                e.getId(),
                e.getCode(),
                e.getTitle(),
                e.getGradeLevel(),
                e.getSubject(),
                e.getType().name(),
                e.getStatus().name(),
                e.getCreatedBy(),
                e.getCreatedAt()
        );
    }
}
