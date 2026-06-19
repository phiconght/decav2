package com.trungtam.identity.dto.request;

import com.trungtam.identity.entity.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * Cap nhat thong tin nguoi dung. Khong doi username, khong doi password.
 * Doi password dung endpoint POST /{id}/reset-password.
 */
public record UpdateUserRequest(
        @Email
        String email,

        @Size(max = 20)
        String phone,

        @Size(max = 150)
        String fullName,

        @NotEmpty(message = "Phai gan it nhat mot vai tro")
        Set<RoleName> roles
) {}
