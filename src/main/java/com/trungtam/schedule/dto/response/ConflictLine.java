package com.trungtam.schedule.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;

/** 1 dong canh bao / chan trung. type: ROOM | TEACHER | STUDENT. */
public record ConflictLine(
        LocalDate date,
        LocalTime startTime,
        String type,
        String resourceName,
        String conflictClassName
) {
}
