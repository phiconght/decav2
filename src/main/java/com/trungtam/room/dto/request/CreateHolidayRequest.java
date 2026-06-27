package com.trungtam.room.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateHolidayRequest(
        @NotNull LocalDate holidayDate,
        @NotBlank String name,
        Long branchId
) {}
