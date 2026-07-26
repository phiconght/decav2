package com.trungtam.schedule.dto.response;

import com.trungtam.schedule.entity.ClassSession;
import com.trungtam.schedule.entity.SessionStatus;

import java.time.LocalDate;
import java.time.LocalTime;

public record SessionDetail(
        Long id,
        Long classId,
        String className,
        LocalDate sessionDate,
        LocalTime startTime,
        LocalTime endTime,
        Long roomId,
        String roomName,
        Long teacherId,
        String teacherName,
        SessionStatus status,
        String cancelReason,
        boolean isManual,
        Long topicId,
        String topicName,
        String title
) {
    public static SessionDetail from(ClassSession s) {
        return new SessionDetail(
                s.getId(),
                s.getClazz().getId(),
                s.getClazz().getName(),
                s.getSessionDate(),
                s.getStartTime(),
                s.endTime(),
                s.getRoom() != null ? s.getRoom().getId() : null,
                s.getRoom() != null ? s.getRoom().getName() : null,
                s.getTeacher() != null ? s.getTeacher().getId() : null,
                s.getTeacher() != null ? s.getTeacher().getFullName() : null,
                s.getStatus(),
                s.getCancelReason(),
                s.isManual(),
                s.getTopic() != null ? s.getTopic().getId() : null,
                s.getTopic() != null ? s.getTopic().getName() : null,
                s.getTitle());
    }
}
