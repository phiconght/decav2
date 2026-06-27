package com.trungtam.schedule.dto.request;

import com.trungtam.schedule.entity.RecurrenceType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Tao / cap nhat quy tac lich. dayOfWeek bat buoc neu WEEKLY (validate o service).
 * endDate null khi ONCE -> ep = startDate.
 */
public record CreateScheduleRequest(
        @NotNull RecurrenceType recurrenceType,
        Short dayOfWeek,
        @NotNull LocalDate startDate,
        LocalDate endDate,
        @NotNull LocalTime startTime,
        @NotNull @Positive Integer durationMinutes,
        Long roomId,
        Long teacherId,
        Boolean active
) {
}
