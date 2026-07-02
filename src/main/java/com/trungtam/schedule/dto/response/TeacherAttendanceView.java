package com.trungtam.schedule.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.trungtam.schedule.entity.SessionTeacherAttendance;

import java.time.Instant;

/**
 * Trang thai cham cong cua 1 buoi. status = "CHUA_CHAM" khi chua co dong.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TeacherAttendanceView(
        Long sessionId,
        Long teacherId,
        String teacherName,
        String status,
        Instant checkInAt,
        Instant checkOutAt,
        String note
) {
    public static TeacherAttendanceView from(SessionTeacherAttendance a) {
        return new TeacherAttendanceView(
                a.getSession().getId(),
                a.getTeacher().getId(),
                a.getTeacher().getFullName(),
                a.getStatus().name(),
                a.getCheckInAt(),
                a.getCheckOutAt(),
                a.getNote());
    }

    /** Buoi chua ai cham cong. */
    public static TeacherAttendanceView notYet(Long sessionId, Long teacherId, String teacherName) {
        return new TeacherAttendanceView(sessionId, teacherId, teacherName, "CHUA_CHAM", null, null, null);
    }
}
