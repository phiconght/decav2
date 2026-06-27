package com.trungtam.schedule.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;

/** 1 dong preview ket qua sinh buoi. blocked = bi chan (trung phong/GV). */
public record SessionPreviewLine(
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        Long roomId,
        String roomName,
        Long teacherId,
        String teacherName,
        boolean blocked
) {
}
