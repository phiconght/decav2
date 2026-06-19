package com.trungtam.identity.dto.response;

import com.trungtam.identity.entity.User;

import java.time.Instant;
import java.util.Set;
import java.util.TreeSet;

/**
 * Dong trong bang danh sach nguoi dung — du lieu toi thieu, khong co permissions.
 */
public record UserListItem(
        Long id,
        String username,
        String fullName,
        String email,
        String phone,
        String status,
        Set<String> roles,
        String createdBy,
        Instant createdAt
) {
    public static UserListItem from(User u) {
        Set<String> roles = new TreeSet<>();
        u.getRoles().forEach(r -> roles.add(r.getName().name()));
        return new UserListItem(
                u.getId(),
                u.getUsername(),
                u.getFullName(),
                u.getEmail(),
                u.getPhone(),
                u.getStatus().name(),
                roles,
                u.getCreatedBy(),
                u.getCreatedAt()
        );
    }
}
