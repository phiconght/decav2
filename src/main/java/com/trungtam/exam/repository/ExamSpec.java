package com.trungtam.exam.repository;

import com.trungtam.exam.dto.request.ExamSearchParams;
import com.trungtam.exam.entity.Exam;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class ExamSpec {

    private ExamSpec() {}

    public static Specification<Exam> build(ExamSearchParams p) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(p.getCode())) {
                predicates.add(cb.like(cb.lower(root.get("code")),
                        "%" + p.getCode().toLowerCase() + "%"));
            }
            if (StringUtils.hasText(p.getName())) {
                predicates.add(cb.like(cb.lower(root.get("name")),
                        "%" + p.getName().toLowerCase() + "%"));
            }
            if (p.getSubjectId() != null) {
                predicates.add(cb.equal(root.get("subject").get("id"), p.getSubjectId()));
            }
            if (StringUtils.hasText(p.getType())) {
                predicates.add(cb.equal(root.get("type").as(String.class), p.getType()));
            }
            if (StringUtils.hasText(p.getStatus())) {
                predicates.add(cb.equal(root.get("status").as(String.class), p.getStatus()));
            }
            if (p.getClassId() != null) {
                query.distinct(true);
                predicates.add(cb.equal(root.join("classes").get("id"), p.getClassId()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
