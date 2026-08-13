package com.trungtam.schedule.dto.response;

import com.trungtam.schedule.entity.AttendanceStatus;

import java.time.Instant;

public record AttendanceItem(
        Long userId,
        String fullName,
        String username,
        String phone,
        AttendanceStatus status,
        Instant checkInAt,
        Instant checkOutAt,
        String confirmedByName,
        Instant confirmedAt
) {
}
