package com.trungtam.schedule.dto.response;

import com.trungtam.schedule.entity.ClassSchedule;
import com.trungtam.schedule.entity.RecurrenceType;

import java.time.LocalDate;
import java.time.LocalTime;

public record ScheduleItem(
        Long id,
        RecurrenceType recurrenceType,
        Short dayOfWeek,
        LocalDate startDate,
        LocalDate endDate,
        LocalTime startTime,
        Integer durationMinutes,
        Long roomId,
        String roomName,
        Long teacherId,
        String teacherName,
        boolean active,
        String defaultZoomUrl
) {
    public static ScheduleItem from(ClassSchedule s) {
        return new ScheduleItem(
                s.getId(),
                s.getRecurrenceType(),
                s.getDayOfWeek(),
                s.getStartDate(),
                s.getEndDate(),
                s.getStartTime(),
                s.getDurationMinutes(),
                s.getRoom() != null ? s.getRoom().getId() : null,
                s.getRoom() != null ? s.getRoom().getName() : null,
                s.getTeacher() != null ? s.getTeacher().getId() : null,
                s.getTeacher() != null ? s.getTeacher().getFullName() : null,
                s.isActive(),
                s.getDefaultZoomUrl());
    }
}
