package com.trungtam.room.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateBranchRequest(
        @NotBlank String code,
        @NotBlank String name,
        String address,
        Boolean active
) {}
