package com.trungtam.schedule.dto.request;

import com.trungtam.schedule.entity.AttendanceStatus;
import jakarta.validation.constraints.NotNull;

/** GV / Admin dat trang thai diem danh thu cong. */
public record UpdateAttendanceRequest(
        @NotNull AttendanceStatus status
) {
}
