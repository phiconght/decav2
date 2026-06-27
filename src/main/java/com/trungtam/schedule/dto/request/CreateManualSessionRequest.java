package com.trungtam.schedule.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.time.LocalTime;

/** Tao buoi hoc thu cong (day bu / buoi le). */
public record CreateManualSessionRequest(
        @NotNull LocalDate sessionDate,
        @NotNull LocalTime startTime,
        @NotNull @Positive Integer durationMinutes,
        Long roomId,
        Long teacherId
) {
}
