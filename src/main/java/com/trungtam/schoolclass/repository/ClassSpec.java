package com.trungtam.schoolclass.repository;

import com.trungtam.schoolclass.dto.request.ClassSearchParams;
import com.trungtam.schoolclass.entity.ClassStatus;
import com.trungtam.schoolclass.entity.SchoolClass;
import org.springframework.data.jpa.domain.Specification;

public final class ClassSpec {

    private ClassSpec() {}

    public static Specification<SchoolClass> build(ClassSearchParams p) {
        return Specification
                .where(likeCode(p.getCode()))
                .and(likeName(p.getName()))
                .and(eqSubjectId(p.getSubjectId()))
                .and(eqStatus(p.getStatus()));
    }

    private static Specification<SchoolClass> likeCode(String code) {
        return (root, q, cb) -> code == null || code.isBlank() ? null
                : cb.like(cb.lower(root.get("code")), "%" + code.toLowerCase() + "%");
    }

    private static Specification<SchoolClass> likeName(String name) {
        return (root, q, cb) -> name == null || name.isBlank() ? null
                : cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    private static Specification<SchoolClass> eqSubjectId(Long subjectId) {
        return (root, q, cb) -> subjectId == null ? null
                : cb.equal(root.get("subject").get("id"), subjectId);
    }

    private static Specification<SchoolClass> eqStatus(String status) {
        return (root, q, cb) -> {
            if (status == null || status.isBlank()) return null;
            try {
                return cb.equal(root.get("status"), ClassStatus.valueOf(status));
            } catch (IllegalArgumentException e) {
                return null;
            }
        };
    }
}
