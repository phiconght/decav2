package com.trungtam.schoolclass.dto.request;

import com.trungtam.schoolclass.entity.ClassStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateClassRequest(
        @NotBlank String name,
        @NotNull Long subjectId,
        LocalDate startDate,
        LocalDate endDate,
        ClassStatus status
) {}
