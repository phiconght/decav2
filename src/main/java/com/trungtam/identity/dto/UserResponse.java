package com.trungtam.identity.dto;

import com.trungtam.identity.entity.User;

import java.time.Instant;
import java.util.Set;
import java.util.TreeSet;

/**
 * Thong tin nguoi dung tra ve client (khong bao gom mat khau).
 */
public record UserResponse(
        Long id,
        String username,
        String email,
        String phone,
        String fullName,
        String status,
        Set<String> roles,
        Set<String> permissions,
        String createdBy,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        Set<String> roles = new TreeSet<>();
        Set<String> permissions = new TreeSet<>();
        user.getRoles().forEach(role -> {
            roles.add(role.getName().name());
            role.getPermissions().forEach(p -> permissions.add(p.getCode()));
        });
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPhone(),
                user.getFullName(),
                user.getStatus().name(),
                roles,
                permissions,
                user.getCreatedBy(),
                user.getCreatedAt());
    }
}
