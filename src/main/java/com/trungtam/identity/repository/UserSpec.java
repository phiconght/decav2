package com.trungtam.identity.repository;

import com.trungtam.identity.dto.request.UserSearchParams;
import com.trungtam.identity.entity.Role;
import com.trungtam.identity.entity.RoleName;
import com.trungtam.identity.entity.User;
import com.trungtam.identity.entity.UserStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

/**
 * JPA Specification cho dong loc nguoi dung theo 5 tieu chi tim kiem.
 * Filter role dung EXISTS subquery de tranh nhan dong va sai count phan trang khi ManyToMany.
 */
public final class UserSpec {

    private UserSpec() {}

    public static Specification<User> build(UserSearchParams p) {
        return Specification
                .where(likeUsername(p.getUsername()))
                .and(likeFullName(p.getFullName()))
                .and(likePhone(p.getPhone()))
                .and(hasRole(p.getRole()))
                .and(eqStatus(p.getStatus()));
    }

    private static Specification<User> likeUsername(String username) {
        return (root, q, cb) -> username == null || username.isBlank() ? null
                : cb.like(cb.lower(root.get("username")), "%" + username.toLowerCase() + "%");
    }

    private static Specification<User> likeFullName(String fullName) {
        return (root, q, cb) -> fullName == null || fullName.isBlank() ? null
                : cb.like(cb.lower(root.get("fullName")), "%" + fullName.toLowerCase() + "%");
    }

    private static Specification<User> likePhone(String phone) {
        return (root, q, cb) -> phone == null || phone.isBlank() ? null
                : cb.like(root.get("phone"), "%" + phone + "%");
    }

    // EXISTS subquery: chon user co it nhat 1 role ten = roleName. Tranh nhân dong khi JOIN ManyToMany.
    private static Specification<User> hasRole(String roleName) {
        return (root, query, cb) -> {
            if (roleName == null || roleName.isBlank()) return null;
            final RoleName target;
            try {
                target = RoleName.valueOf(roleName);
            } catch (IllegalArgumentException e) {
                return cb.disjunction(); // role khong hop le -> khong khop ai
            }
            Subquery<Long> sub = query.subquery(Long.class);
            Root<User> subUser = sub.from(User.class);
            Join<User, Role> subRoles = subUser.join("roles");
            sub.select(cb.literal(1L))
               .where(
                   cb.equal(subUser, root),                  // tuong quan voi user o ngoai
                   cb.equal(subRoles.get("name"), target)
               );
            return cb.exists(sub);
        };
    }

    private static Specification<User> eqStatus(String status) {
        return (root, q, cb) -> {
            if (status == null || status.isBlank()) return null;
            try {
                return cb.equal(root.get("status"), UserStatus.valueOf(status));
            } catch (IllegalArgumentException e) {
                return null;
            }
        };
    }
}
