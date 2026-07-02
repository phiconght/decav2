package com.trungtam.schedule.dto.request;

import com.trungtam.schedule.entity.TeacherAttendanceStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Admin cham cong tay cho GV (khi QR hong / buoi chua xep phong). */
public record SetTeacherAttendanceRequest(
        @NotNull TeacherAttendanceStatus status,
        @Size(max = 255) String note
) {
}
