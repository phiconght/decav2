package com.trungtam.leave.dto.request;

import com.trungtam.leave.entity.LeaveScope;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Yeu cau tao don xin nghi.
 * SESSION -> can sessionId; RANGE -> can dateFrom <= dateTo (classId null = tat ca lop).
 */
public record CreateLeaveRequest(
        @NotNull Long studentId,
        @NotNull LeaveScope scope,
        Long sessionId,
        Long classId,
        LocalDate dateFrom,
        LocalDate dateTo,
        String reason
) {
}
