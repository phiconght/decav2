package com.trungtam.room.repository;

import com.trungtam.room.dto.request.RoomSearchParams;
import com.trungtam.room.entity.Room;
import org.springframework.data.jpa.domain.Specification;

/** JPA Specification loc phong hoc theo co so / keyword / trang thai. */
public final class RoomSpec {

    private RoomSpec() {}

    public static Specification<Room> build(RoomSearchParams p) {
        return Specification
                .where(eqBranch(p.getBranchId()))
                .and(likeKeyword(p.getKeyword()))
                .and(eqActive(p.getActive()));
    }

    private static Specification<Room> eqBranch(Long branchId) {
        return (root, q, cb) -> branchId == null ? null
                : cb.equal(root.get("branch").get("id"), branchId);
    }

    private static Specification<Room> likeKeyword(String keyword) {
        return (root, q, cb) -> {
            if (keyword == null || keyword.isBlank()) return null;
            String kw = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("code")), kw),
                    cb.like(cb.lower(root.get("name")), kw));
        };
    }

    private static Specification<Room> eqActive(Boolean active) {
        return (root, q, cb) -> active == null ? null : cb.equal(root.get("active"), active);
    }
}
