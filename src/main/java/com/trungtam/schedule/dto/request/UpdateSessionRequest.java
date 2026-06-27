package com.trungtam.schedule.dto.request;

import java.time.LocalTime;

/** Sua 1 buoi hoc le (gio / thoi luong / phong / GV). */
public record UpdateSessionRequest(
        LocalTime startTime,
        Integer durationMinutes,
        Long roomId,
        Long teacherId
) {
}
