package com.trungtam.exam.dto.response;

import com.trungtam.identity.entity.User;

public record StudentOptionResponse(Long id, String username, String fullName) {
    public static StudentOptionResponse from(User u) {
        return new StudentOptionResponse(u.getId(), u.getUsername(), u.getFullName());
    }
}
