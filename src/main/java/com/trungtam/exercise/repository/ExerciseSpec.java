package com.trungtam.exercise.repository;

import com.trungtam.exercise.dto.request.ExerciseSearchParams;
import com.trungtam.exercise.entity.Exercise;
import com.trungtam.exercise.entity.ExerciseStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * JPA Specification cho dong loc bai tap theo 7 tieu chi tim kiem.
 */
public final class ExerciseSpec {

    private ExerciseSpec() {}

    public static Specification<Exercise> build(ExerciseSearchParams p) {
        return Specification
                .where(likeCode(p.getCode()))
                .and(eqGradeLevel(p.getGradeLevel()))
                .and(eqSubject(p.getSubject()))
                .and(likeCreatedBy(p.getCreatedBy()))
                .and(fromDate(p.getCreatedFrom()))
                .and(toDate(p.getCreatedTo()))
                .and(eqStatus(p.getStatus()));
    }

    private static Specification<Exercise> likeCode(String code) {
        return (root, q, cb) -> code == null || code.isBlank() ? null
                : cb.like(cb.lower(root.get("code")), "%" + code.toLowerCase() + "%");
    }

    private static Specification<Exercise> eqGradeLevel(String gradeLevel) {
        return (root, q, cb) -> gradeLevel == null || gradeLevel.isBlank() ? null
                : cb.equal(root.get("gradeLevel"), gradeLevel);
    }

    private static Specification<Exercise> eqSubject(String subject) {
        return (root, q, cb) -> subject == null || subject.isBlank() ? null
                : cb.equal(root.get("subject"), subject);
    }

    private static Specification<Exercise> likeCreatedBy(String createdBy) {
        return (root, q, cb) -> createdBy == null || createdBy.isBlank() ? null
                : cb.like(cb.lower(root.get("createdBy")), "%" + createdBy.toLowerCase() + "%");
    }

    private static Specification<Exercise> fromDate(String dateStr) {
        return (root, q, cb) -> {
            if (dateStr == null || dateStr.isBlank()) return null;
            var from = LocalDate.parse(dateStr).atStartOfDay(ZoneOffset.UTC).toInstant();
            return cb.greaterThanOrEqualTo(root.get("createdAt"), from);
        };
    }

    private static Specification<Exercise> toDate(String dateStr) {
        return (root, q, cb) -> {
            if (dateStr == null || dateStr.isBlank()) return null;
            // Den het ngay cuoi (exclusive: sang hom sau)
            var to = LocalDate.parse(dateStr).plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            return cb.lessThan(root.get("createdAt"), to);
        };
    }

    private static Specification<Exercise> eqStatus(String status) {
        return (root, q, cb) -> {
            if (status == null || status.isBlank()) return null;
            try {
                return cb.equal(root.get("status"), ExerciseStatus.valueOf(status));
            } catch (IllegalArgumentException e) {
                return null;
            }
        };
    }
}
