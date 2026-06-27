package com.trungtam.schedule.dto.response;

import com.trungtam.schedule.entity.SessionStatus;

import java.time.LocalDate;
import java.time.LocalTime;

public record TimetableItem(
        Long sessionId,
        Long classId,
        String className,
        String subjectName,
        String gradeLevel,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        Long roomId,
        String roomName,
        String branchName,
        Long teacherId,
        String teacherName,
        SessionStatus status,
        // chi view STUDENT / PARENT:
        Long studentId,
        String studentName,
        String attendanceStatus,
        boolean onLeave
) {
}
