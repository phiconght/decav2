package com.trungtam.identity.dto;

import com.trungtam.identity.entity.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateUserRequest(
        @NotBlank @Size(min = 3, max = 100)
        String username,

        @Email
        String email,

        @Size(max = 20)
        String phone,

        @NotBlank @Size(min = 6, max = 100)
        String password,

        @Size(max = 150)
        String fullName,

        @NotEmpty(message = "Phai gan it nhat mot vai tro")
        Set<RoleName> roles
) {
}
