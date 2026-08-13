package com.trungtam.video.repository;

import com.trungtam.video.dto.request.LectureVideoSearchParams;
import com.trungtam.video.entity.LectureVideo;
import org.springframework.data.jpa.domain.Specification;

/** JPA Specification loc kho video theo chuyen de / tu khoa. */
public final class LectureVideoSpec {

    private LectureVideoSpec() {}

    public static Specification<LectureVideo> build(LectureVideoSearchParams p) {
        return Specification
                .where(eqTopic(p.getTopicId()))
                .and(likeKeyword(p.getQ()));
    }

    private static Specification<LectureVideo> eqTopic(Long topicId) {
        return (root, q, cb) -> topicId == null ? null
                : cb.equal(root.get("topic").get("id"), topicId);
    }

    private static Specification<LectureVideo> likeKeyword(String keyword) {
        return (root, q, cb) -> {
            if (keyword == null || keyword.isBlank()) return null;
            String kw = "%" + keyword.toLowerCase() + "%";
            return cb.like(cb.lower(root.get("title")), kw);
        };
    }
}
