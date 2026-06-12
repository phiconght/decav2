package com.trungtam.identity.dto;

import com.trungtam.identity.entity.RoleName;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record AssignRolesRequest(
        @NotEmpty(message = "Danh sach vai tro khong duoc rong")
        Set<RoleName> roles
) {
}
