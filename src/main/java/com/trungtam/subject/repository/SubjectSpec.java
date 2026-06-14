package com.trungtam.subject.repository;

import com.trungtam.subject.dto.request.SubjectSearchParams;
import com.trungtam.subject.entity.Subject;
import org.springframework.data.jpa.domain.Specification;

public final class SubjectSpec {

    private SubjectSpec() {}

    public static Specification<Subject> build(SubjectSearchParams p) {
        return Specification
                .where(likeCode(p.getCode()))
                .and(eqGradeLevel(p.getGradeLevel()));
    }

    private static Specification<Subject> likeCode(String code) {
        return (root, q, cb) -> code == null || code.isBlank() ? null
                : cb.like(cb.lower(root.get("code")), "%" + code.toLowerCase() + "%");
    }

    private static Specification<Subject> eqGradeLevel(String gradeLevel) {
        return (root, q, cb) -> gradeLevel == null || gradeLevel.isBlank() ? null
                : cb.equal(root.get("gradeLevel"), gradeLevel);
    }
}
