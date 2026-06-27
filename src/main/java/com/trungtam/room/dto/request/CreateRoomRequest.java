package com.trungtam.room.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateRoomRequest(
        @NotBlank String code,
        @NotBlank String name,
        @NotNull Long branchId,
        Integer capacity,
        String note,
        Boolean active
) {}
