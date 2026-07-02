package com.trungtam.schedule.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/** 1 dong bao cao cong: 1 buoi day cua GV. status co the la CHUA_CHAM. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TeacherWorkItem(
        Long sessionId,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        String className,
        String roomName,
        Integer durationMinutes,
        String status,          // DUNG_GIO | VAO_TRE | VANG | CHUA_CHAM
        Instant checkInAt,
        Instant checkOutAt,
        String note
) {
}
