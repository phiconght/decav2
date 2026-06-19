package com.trungtam.identity.dto.request;

import com.trungtam.identity.entity.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(
        @NotNull(message = "Trang thai khong duoc de trong")
        UserStatus status
) {}
